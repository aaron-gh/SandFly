// Run with macOS JavaScriptCore:
// /System/Library/Frameworks/JavaScriptCore.framework/Versions/Current/Helpers/jsc -m test/report_test.mjs

import { issueBody, labels, logComment, parseReport } from '../src/report.js';

let failures = 0;
function check(name, condition) {
  if (!condition) {
    failures++;
    print(`FAIL ${name}`);
  }
}
function rejects(name, json, field) {
  try {
    parseReport(json);
    check(`${name} is rejected`, false);
  } catch (e) {
    check(`${name} names ${field}, got ${e.message}`, e.message === field);
  }
}

const valid = {
  type: 'Incorrect behavior',
  area: 'Speech',
  title: ' Focus skips a row ',
  app: 'Chrome',
  description: 'Ask @someone, mail me at a@b.co\n',
  steps: '1. Open\n2. Swipe',
  email: 'reporter@example.com',
  version: 'SandFly 0.1.2, build 790',
  device: ['Android 16 (API 36)', 'samsung SM-F966B'],
  log: 'line one\n````\nline three\n',
};

const report = parseReport(valid);
check('title is trimmed', report.title === 'Focus skips a row');
check('labels for a bug', labels(report).join() === 'bug,user report');
check(
  'suggestion label',
  labels(parseReport({ ...valid, type: 'Suggestion' }))[0] === 'enhancement',
);

const body = issueBody(report);
check('body has type', body.startsWith('**Type:** Incorrect behavior\n**Area:** Speech\n'));
check('body has email in code', body.includes('**Email:** `reporter@example.com`'));
check('mention is broken', body.includes('Ask @​someone'));
check('email in prose is kept', body.includes('a@b.co'));
check('body has steps', body.includes('### Steps to reproduce\n\n1. Open\n2. Swipe\n'));
check('body has device', body.includes('- SandFly 0.1.2, build 790\n- Android 16 (API 36)\n'));
check('body points to the log', body.includes('first comment'));

const suggestion = issueBody(parseReport({ ...valid, type: 'Suggestion', log: '' }));
check('suggestion has no steps', !suggestion.includes('Steps to reproduce'));
check('no log, no pointer', !suggestion.includes('first comment'));

const minimal = parseReport({ type: 'Crash', title: 'T', description: 'D' });
check('optional fields empty', minimal.area === '' && minimal.device.length === 0);
check('minimal body', issueBody(minimal) === '**Type:** Crash\n\n### Description\n\nD\n');

const comment = logComment(report);
check('log fence is longer than any run', comment.includes('`````text\nline one\n````\nline three\n`````\n'));

rejects('missing type', { ...valid, type: undefined }, 'type');
rejects('unknown type', { ...valid, type: 'Other' }, 'type');
rejects('unknown area', { ...valid, area: 'Elsewhere' }, 'area');
rejects('blank title', { ...valid, title: '  ' }, 'title');
rejects('two-line title', { ...valid, title: 'a\nb' }, 'title');
rejects('long title', { ...valid, title: 'x'.repeat(201) }, 'title');
rejects('missing description', { ...valid, description: undefined }, 'description');
rejects('number as text', { ...valid, app: 5 }, 'app');
rejects('bad email', { ...valid, email: 'not an email' }, 'email');
rejects('device not a list', { ...valid, device: 'x' }, 'device');
rejects('too many device lines', { ...valid, device: Array(11).fill('x') }, 'device');
rejects('long log', { ...valid, log: 'x'.repeat(60001) }, 'log');
rejects('array report', [], 'report');
rejects('null report', null, 'report');

print(failures ? `${failures} failed` : 'All passed');
