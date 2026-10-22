// The conversation about a report: what SandFly shows the reporter of the issue and its comments.

/** Marks a comment that the reporter wrote in SandFly. */
export const USER_MARKER = '<!-- sandfly-user -->';
/** Marks the comment that holds the SandFly log, which the reporter doesn't see. */
export const LOG_MARKER = '<!-- sandfly-log -->';
/** A developer comment that starts with this stays on GitHub and isn't shown to the reporter. */
export const PRIVATE_PREFIX = '[private]';

export const MAX_REPLY = 10000;
// Each token takes two GitHub requests, and a Worker on the free plan may make 50.
export const MAX_TOKENS = 20;
const TOKEN = /^[A-Za-z0-9_-]{32,64}$/;

/** The hidden line in the issue body that names the report, for finding it again. */
export function reportIdLine(id) {
  return `<!-- sandfly-report: ${id} -->`;
}

/** Returns the reply text from parsed JSON, or throws an Error naming the bad field. */
export function parseReply(json) {
  if (json === null || typeof json !== 'object' || Array.isArray(json)) throw new Error('reply');
  const token = parseToken(json.token);
  if (typeof json.body !== 'string') throw new Error('body');
  const body = json.body.trim();
  if (!body || body.length > MAX_REPLY) throw new Error('body');
  return { token, body };
}

/** Returns the tokens from parsed JSON, or throws an Error naming the bad field. */
export function parseTokens(json) {
  if (json === null || typeof json !== 'object' || !Array.isArray(json.tokens)) {
    throw new Error('tokens');
  }
  if (json.tokens.length > MAX_TOKENS) throw new Error('tokens');
  return json.tokens.map(parseToken);
}

function parseToken(value) {
  if (typeof value !== 'string' || !TOKEN.test(value)) throw new Error('token');
  return value;
}

/** The comment that carries a reply the reporter wrote in SandFly. */
export function userComment(body) {
  return `${USER_MARKER}\n**Reporter's reply, sent from SandFly:**\n\n${noMentions(body)}\n`;
}

/**
 * The message the reporter sees for a GitHub comment, or null for one they don't: the log, and
 * developer comments that start with [PRIVATE_PREFIX].
 */
export function messageOf(comment) {
  const body = comment.body || '';
  if (body.includes(LOG_MARKER) || body.startsWith('SandFly log\n')) return null;
  if (body.startsWith(USER_MARKER)) {
    const text = body
      .slice(USER_MARKER.length)
      .replace(/^\s*\*\*Reporter's reply, sent from SandFly:\*\*\s*/, '')
      .trim();
    return { id: comment.id, from: 'reporter', body: unbreakMentions(text), at: comment.created_at };
  }
  if (body.trimStart().toLowerCase().startsWith(PRIVATE_PREFIX)) return null;
  return { id: comment.id, from: 'developer', body: body.trim(), at: comment.created_at };
}

/** The status the reporter sees: open, or closed with GitHub's reason. */
export function statusOf(issue) {
  if (issue.state !== 'closed') return 'open';
  switch (issue.state_reason) {
    case 'not_planned':
      return 'not_planned';
    case 'duplicate':
      return 'duplicate';
    default:
      return 'completed';
  }
}

/** The thread for one report, as SandFly receives it. */
export function threadOf(token, issue, comments) {
  return {
    token,
    status: statusOf(issue),
    updated: issue.updated_at,
    messages: comments.map(messageOf).filter((m) => m !== null),
  };
}

// Stops @names in the reporter's text from notifying GitHub users, as in report.js.
function noMentions(value) {
  return value.replace(/(^|[^\w`])@(?=\w)/g, '$1@​');
}

function unbreakMentions(value) {
  return value.replace(/@​/g, '@');
}
