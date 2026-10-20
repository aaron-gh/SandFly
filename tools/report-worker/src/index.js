// Cloudflare Worker that turns problem reports sent from SandFly into GitHub issues, so that
// reporters need no GitHub account. Only this Worker holds the GitHub token.

import { issueBody, issueTitle, labels, logComment, parseReport } from './report.js';

// Twice the largest report: every field at its limit, with JSON escaping.
const MAX_REQUEST_BYTES = 200_000;
const PER_ADDRESS_PER_DAY = 10;
const ALL_PER_DAY = 100;
const DAY_SECONDS = 24 * 60 * 60;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== '/report') return reply(404, 'not_found');
    if (request.method !== 'POST') return reply(405, 'method');
    if (!(request.headers.get('content-type') || '').startsWith('application/json')) {
      return reply(415, 'content_type');
    }

    let report;
    try {
      const body = await readLimited(request, MAX_REQUEST_BYTES);
      if (body === null) return reply(413, 'too_large');
      report = parseReport(JSON.parse(body));
    } catch (e) {
      return reply(400, e instanceof SyntaxError ? 'json' : `field:${e.message}`);
    }

    const address = request.headers.get('cf-connecting-ip') || 'unknown';
    const { success } = await env.PER_ADDRESS.limit({ key: address });
    if (!success || !(await withinDailyLimits(env, address))) return reply(429, 'rate_limited');

    const issue = await github(env, `/repos/${env.REPO}/issues`, {
      title: issueTitle(report),
      body: issueBody(report),
      labels: labels(report),
    });
    if (!issue.ok) {
      console.error('Issue not created', issue.status, await issue.text());
      return reply(502, 'github');
    }
    if (report.log) {
      const { number } = await issue.json();
      const comment = await github(env, `/repos/${env.REPO}/issues/${number}/comments`, {
        body: logComment(report),
      });
      if (!comment.ok) console.error('Log comment not added', comment.status, await comment.text());
    }
    return new Response(JSON.stringify({ sent: true }), {
      status: 201,
      headers: { 'content-type': 'application/json' },
    });
  },
};

/** Returns the request body as text, or null if it is longer than [limit] bytes. */
async function readLimited(request, limit) {
  const declared = Number(request.headers.get('content-length') || 0);
  if (declared > limit) return null;
  if (!request.body) return '';
  const reader = request.body.getReader();
  const chunks = [];
  let size = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    size += value.byteLength;
    if (size > limit) {
      await reader.cancel();
      return null;
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(size);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return new TextDecoder('utf-8', { fatal: true }).decode(bytes);
}

/**
 * Counts the report against the daily limits for its address and for all reports. KV counts are
 * eventually consistent, so a burst can pass a limit by a few reports.
 */
async function withinDailyLimits(env, address) {
  const day = new Date().toISOString().slice(0, 10);
  const addressKey = `${day}:${await sha256(address)}`;
  const allKey = `${day}:all`;
  const [forAddress, forAll] = await Promise.all([count(env, addressKey), count(env, allKey)]);
  if (forAddress >= PER_ADDRESS_PER_DAY || forAll >= ALL_PER_DAY) return false;
  const options = { expirationTtl: 2 * DAY_SECONDS };
  await Promise.all([
    env.COUNTS.put(addressKey, String(forAddress + 1), options),
    env.COUNTS.put(allKey, String(forAll + 1), options),
  ]);
  return true;
}

async function count(env, key) {
  return Number((await env.COUNTS.get(key)) || 0);
}

async function sha256(value) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

function github(env, path, body) {
  return fetch(`https://api.github.com${path}`, {
    method: 'POST',
    headers: {
      accept: 'application/vnd.github+json',
      authorization: `Bearer ${env.GITHUB_TOKEN}`,
      'content-type': 'application/json',
      'user-agent': 'sandfly-reports',
      'x-github-api-version': '2022-11-28',
    },
    body: JSON.stringify(body),
  });
}

function reply(status, error) {
  return new Response(JSON.stringify({ error }), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}
