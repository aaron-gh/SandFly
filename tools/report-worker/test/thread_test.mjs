// Run with macOS JavaScriptCore:
// /System/Library/Frameworks/JavaScriptCore.framework/Versions/Current/Helpers/jsc -m test/thread_test.mjs

import {
  LOG_MARKER,
  messageOf,
  parseReply,
  parseTokens,
  statusOf,
  threadOf,
  userComment,
} from '../src/thread.js';
import { issueBody, logComment, parseReport } from '../src/report.js';

let failures = 0;
function check(name, condition) {
  if (!condition) {
    failures++;
    print(`FAIL ${name}`);
  }
}
function throws(name, f, field) {
  try {
    f();
    check(`${name} is rejected`, false);
  } catch (e) {
    check(`${name} names ${field}, got ${e.message}`, e.message === field);
  }
}

const token = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ';

// Replies.
const parsed = parseReply({ token, body: '  Still happens on build 140 \n' });
check('reply is trimmed', parsed.body === 'Still happens on build 140');
throws('empty reply', () => parseReply({ token, body: '  ' }), 'body');
throws('long reply', () => parseReply({ token, body: 'x'.repeat(10001) }), 'body');
throws('bad token', () => parseReply({ token: 'short', body: 'x' }), 'token');
throws('token with a slash', () => parseReply({ token: token.replace('a', '/'), body: 'x' }), 'token');
check('tokens', parseTokens({ tokens: [token] }).length === 1);
throws('too many tokens', () => parseTokens({ tokens: Array(21).fill(token) }), 'tokens');
throws('no tokens', () => parseTokens({}), 'tokens');

// Comments as the reporter sees them.
const fromReporter = messageOf({ id: 1, body: userComment('Ask @someone'), created_at: 't1' });
check('reporter message', fromReporter.from === 'reporter');
check('reporter message text', fromReporter.body === 'Ask @someone');
check('mention broken on GitHub', userComment('Ask @someone').includes('@​someone'));
const fromDeveloper = messageOf({ id: 2, body: 'Fixed in build 150.\n', created_at: 't2' });
check('developer message', fromDeveloper.from === 'developer' && fromDeveloper.body === 'Fixed in build 150.');
check('private note hidden', messageOf({ id: 3, body: '[Private] cause is X' }) === null);
check('old log hidden', messageOf({ id: 4, body: 'SandFly log\n\n```text\nx\n```\n' }) === null);
const report = parseReport({ type: 'Crash', title: 't', description: 'd', log: 'x' });
check('new log hidden', messageOf({ id: 5, body: logComment(report) }) === null);
check('log has marker', logComment(report).startsWith(LOG_MARKER));
check('report id hidden in body', issueBody(report, 'ab12').startsWith('<!-- sandfly-report: ab12 -->\n'));
check('no id without one', issueBody(report).startsWith('**Type:**'));

// Status.
check('open', statusOf({ state: 'open' }) === 'open');
check('completed', statusOf({ state: 'closed', state_reason: 'completed' }) === 'completed');
check('not planned', statusOf({ state: 'closed', state_reason: 'not_planned' }) === 'not_planned');
check('duplicate', statusOf({ state: 'closed', state_reason: 'duplicate' }) === 'duplicate');
const thread = threadOf(token, { state: 'open', updated_at: 'u' }, [
  { id: 1, body: logComment(report) },
  { id: 2, body: 'Can you send a log?' },
]);
check('thread messages', thread.messages.length === 1 && thread.messages[0].id === 2);

print(failures === 0 ? 'All thread tests passed' : `${failures} failed`);
