// Validates a SandFly problem report and writes it as a GitHub issue.

import { LOG_MARKER, reportIdLine } from './thread.js';

export const TYPES = ['Incorrect behavior', 'Crash', 'Slow or unresponsive', 'Suggestion'];
export const AREAS = [
  'Speech',
  'Gestures',
  'Touch exploration',
  'Reading controls and menus',
  'Typing and keyboards',
  'Braille keyboard',
  'Braille display',
  'Sounds and vibration',
  'Notifications',
  'Web content',
  'Scripts',
  'Settings',
  'Updates',
  'Other',
];

const LIMITS = {
  title: 200,
  app: 100,
  description: 10000,
  steps: 10000,
  email: 254,
  version: 100,
  deviceLine: 200,
  deviceLines: 10,
  log: 400000,
};
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Returns the report from parsed JSON, or throws an Error whose message names the bad field. */
export function parseReport(json) {
  if (json === null || typeof json !== 'object' || Array.isArray(json)) {
    throw new Error('report');
  }
  const report = {
    type: oneOf(json.type, TYPES, 'type', true),
    area: oneOf(json.area, AREAS, 'area', false),
    title: text(json.title, LIMITS.title, 'title', true),
    app: text(json.app, LIMITS.app, 'app', false),
    description: text(json.description, LIMITS.description, 'description', true),
    steps: text(json.steps, LIMITS.steps, 'steps', false),
    email: text(json.email, LIMITS.email, 'email', false),
    version: text(json.version, LIMITS.version, 'version', false),
    device: lines(json.device),
    log: text(json.log, LIMITS.log, 'log', false, false),
  };
  if (report.email && !EMAIL.test(report.email)) {
    throw new Error('email');
  }
  if (report.title.includes('\n')) {
    throw new Error('title');
  }
  return report;
}

function oneOf(value, allowed, field, required) {
  if (value === undefined || value === null || value === '') {
    if (required) throw new Error(field);
    return '';
  }
  if (!allowed.includes(value)) throw new Error(field);
  return value;
}

function text(value, limit, field, required, trim = true) {
  if (value === undefined || value === null) value = '';
  if (typeof value !== 'string' || value.length > limit) throw new Error(field);
  const result = trim ? value.trim() : value;
  if (required && !result) throw new Error(field);
  return result;
}

function lines(value) {
  if (value === undefined || value === null) return [];
  if (!Array.isArray(value) || value.length > LIMITS.deviceLines) throw new Error('device');
  return value.map((line) => {
    const result = text(line, LIMITS.deviceLine, 'device', true);
    if (result.includes('\n')) throw new Error('device');
    return result;
  });
}

/** The labels for the issue. */
export function labels(report) {
  return [report.type === 'Suggestion' ? 'enhancement' : 'bug', 'user report'];
}

/** The issue body in GitHub Markdown, with the hidden ID of the report if there is one. */
export function issueBody(report, id) {
  let body = id ? `${reportIdLine(id)}\n` : '';
  body += `**Type:** ${report.type}\n`;
  if (report.area) body += `**Area:** ${report.area}\n`;
  if (report.app) body += `**App:** ${inline(report.app)}\n`;
  if (report.email) body += `**Email:** \`${report.email}\`\n`;
  body += `\n### Description\n\n${prose(report.description)}\n`;
  if (report.steps && report.type !== 'Suggestion') {
    body += `\n### Steps to reproduce\n\n${prose(report.steps)}\n`;
  }
  if (report.device.length || report.version) {
    body += '\n### Device\n\n';
    if (report.version) body += `- ${inline(report.version)}\n`;
    for (const line of report.device) body += `- ${inline(line)}\n`;
  }
  if (report.log) body += '\nThe SandFly log is in the first comment, or the first few when it is long.\n';
  return body;
}

/** The issue title. */
export function issueTitle(report) {
  return report.title;
}

/** The most of the log in one comment. GitHub takes 65,536 characters, with the heading. */
export const LOG_PART = 60000;

/**
 * The comments with the SandFly log, in order, each holding at most LOG_PART characters of it, cut
 * between lines where it can be, in a code block that no line of the log can close.
 */
export function logComments(report) {
  const log = report.log.replace(/\s+$/, '');
  const parts = [];
  let start = 0;
  while (start < log.length) {
    let end = Math.min(start + LOG_PART, log.length);
    if (end < log.length) {
      const newline = log.lastIndexOf('\n', end - 1);
      if (newline > start) end = newline + 1;
    }
    parts.push(log.slice(start, end).replace(/\n$/, ''));
    start = end;
  }
  return parts.map((part, i) => {
    const longestRun = Math.max(2, ...(part.match(/`+/g) || []).map((run) => run.length));
    const fence = '`'.repeat(longestRun + 1);
    const heading = parts.length > 1 ? `SandFly log, part ${i + 1} of ${parts.length}` : 'SandFly log';
    return `${LOG_MARKER}\n${heading}\n\n${fence}text\n${part}\n${fence}\n`;
  });
}

// Stops @names in the reporter's text from notifying GitHub users.
function noMentions(value) {
  return value.replace(/(^|[^\w`])@(?=\w)/g, '$1@​');
}

function inline(value) {
  return noMentions(value.replace(/\s+/g, ' '));
}

function prose(value) {
  return noMentions(value);
}
