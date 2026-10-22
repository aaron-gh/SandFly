// Cloudflare Worker that turns problem reports sent from SandFly into GitHub issues, so that
// reporters need no GitHub account, and carries the conversation about each report between the
// issue and SandFly. Only this Worker holds the GitHub token.
//
// Each report gets a secret token that only the reporter's SandFly holds. The Worker keeps the
// SHA-256 of each token in KV, with the issue number, so the token itself is stored nowhere else.

import { issueBody, issueTitle, labels, logComment, parseReport } from './report.js';
import { parseReply, parseTokens, threadOf, userComment } from './thread.js';

// Twice the largest report: every field at its limit, with JSON escaping.
const MAX_REQUEST_BYTES = 200_000;
const MAX_SMALL_REQUEST_BYTES = 40_000;
const PER_ADDRESS_PER_DAY = 10;
const ALL_PER_DAY = 100;
const REPLIES_PER_ADDRESS_PER_DAY = 30;
const DAY_SECONDS = 24 * 60 * 60;
// The comments of a report that are read, which is more than any report has had.
const MAX_COMMENTS = 100;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const route = ROUTES[url.pathname];
    if (!route) return reply(404, 'not_found');
    if (request.method !== 'POST') return reply(405, 'method');
    if (!(request.headers.get('content-type') || '').startsWith('application/json')) {
      return reply(415, 'content_type');
    }
    let json;
    try {
      const body = await readLimited(request, route.maxBytes);
      if (body === null) return reply(413, 'too_large');
      json = JSON.parse(body);
    } catch (e) {
      return reply(400, 'json');
    }
    return route.handle(json, request, env);
  },
};

const ROUTES = {
  '/report': { maxBytes: MAX_REQUEST_BYTES, handle: createReport },
  '/threads': { maxBytes: MAX_SMALL_REQUEST_BYTES, handle: readThreads },
  '/reply': { maxBytes: MAX_SMALL_REQUEST_BYTES, handle: addReply },
};

/** Creates the issue, and returns the token that SandFly uses to follow it. */
async function createReport(json, request, env) {
  let report;
  try {
    report = parseReport(json);
  } catch (e) {
    return reply(400, `field:${e.message}`);
  }
  const address = request.headers.get('cf-connecting-ip') || 'unknown';
  const { success } = await env.PER_ADDRESS.limit({ key: address });
  if (!success || !(await withinDailyLimits(env, address))) return reply(429, 'rate_limited');

  const id = randomHex(8);
  const issue = await github(env, 'POST', `/repos/${env.REPO}/issues`, {
    title: issueTitle(report),
    body: issueBody(report, id),
    labels: labels(report),
  });
  if (!issue.ok) {
    console.error('Issue not created', issue.status, await issue.text());
    return reply(502, 'github');
  }
  const { number } = await issue.json();
  if (report.log) {
    const comment = await github(env, 'POST', `/repos/${env.REPO}/issues/${number}/comments`, {
      body: logComment(report),
    });
    if (!comment.ok) console.error('Log comment not added', comment.status, await comment.text());
  }
  const token = randomToken();
  await env.THREADS.put(`token:${await sha256(token)}`, JSON.stringify({ number, id }));
  return json201({ sent: true, token });
}

/** Returns the status and messages of each report whose token SandFly sends. */
async function readThreads(json, request, env) {
  let tokens;
  try {
    tokens = parseTokens(json);
  } catch (e) {
    return reply(400, `field:${e.message}`);
  }
  const threads = [];
  for (const token of tokens) {
    const number = await issueNumber(env, token);
    if (number === null) {
      threads.push({ token, status: 'unknown', messages: [] });
      continue;
    }
    const [issue, comments] = await Promise.all([
      github(env, 'GET', `/repos/${env.REPO}/issues/${number}`),
      github(env, 'GET', `/repos/${env.REPO}/issues/${number}/comments?per_page=${MAX_COMMENTS}`),
    ]);
    if (!issue.ok || !comments.ok) {
      console.error('Thread not read', number, issue.status, comments.status);
      return reply(502, 'github');
    }
    threads.push(threadOf(token, await issue.json(), await comments.json()));
  }
  return new Response(JSON.stringify({ threads }), {
    status: 200,
    headers: { 'content-type': 'application/json' },
  });
}

/** Adds the reporter's reply to the issue as a comment. */
async function addReply(json, request, env) {
  let parsed;
  try {
    parsed = parseReply(json);
  } catch (e) {
    return reply(400, `field:${e.message}`);
  }
  const address = request.headers.get('cf-connecting-ip') || 'unknown';
  const { success } = await env.PER_ADDRESS.limit({ key: `reply:${address}` });
  if (!success || !(await withinReplyLimit(env, address))) return reply(429, 'rate_limited');
  const number = await issueNumber(env, parsed.token);
  if (number === null) return reply(404, 'unknown_report');
  const comment = await github(env, 'POST', `/repos/${env.REPO}/issues/${number}/comments`, {
    body: userComment(parsed.body),
  });
  if (!comment.ok) {
    console.error('Reply not added', comment.status, await comment.text());
    return reply(502, 'github');
  }
  const { id, created_at } = await comment.json();
  return json201({ sent: true, id, at: created_at });
}

async function issueNumber(env, token) {
  const stored = await env.THREADS.get(`token:${await sha256(token)}`);
  if (!stored) return null;
  try {
    return JSON.parse(stored).number;
  } catch (e) {
    return null;
  }
}

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

/** Counts the reply against the daily limit for its address. */
async function withinReplyLimit(env, address) {
  const day = new Date().toISOString().slice(0, 10);
  const key = `${day}:reply:${await sha256(address)}`;
  const replies = await count(env, key);
  if (replies >= REPLIES_PER_ADDRESS_PER_DAY) return false;
  await env.COUNTS.put(key, String(replies + 1), { expirationTtl: 2 * DAY_SECONDS });
  return true;
}

async function count(env, key) {
  return Number((await env.COUNTS.get(key)) || 0);
}

async function sha256(value) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

function randomHex(bytes) {
  return [...crypto.getRandomValues(new Uint8Array(bytes))]
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/** 32 random bytes, in base64url: 43 characters. */
function randomToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function github(env, method, path, body) {
  return fetch(`${env.GITHUB_API || 'https://api.github.com'}${path}`, {
    method,
    headers: {
      accept: 'application/vnd.github+json',
      authorization: `Bearer ${env.GITHUB_TOKEN}`,
      'content-type': 'application/json',
      'user-agent': 'sandfly-reports',
      'x-github-api-version': '2022-11-28',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

function json201(value) {
  return new Response(JSON.stringify(value), {
    status: 201,
    headers: { 'content-type': 'application/json' },
  });
}

function reply(status, error) {
  return new Response(JSON.stringify({ error }), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}
