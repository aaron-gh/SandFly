# SandFly report server

A Cloudflare Worker that receives the reports from Settings > Send feedback and creates
issues in a private GitHub repository. Reporters need no GitHub account. The GitHub token is
stored only in the Worker.

SandFly posts JSON to `https://<worker>/report`. The Worker:

- accepts at most 200 KB, checks every field against the same limits as the app, and returns 400
  for a field that fails;
- allows 2 reports a minute and 10 a day from one address, and 100 a day in total (429 when over);
- creates the issue with the labels `bug` or `enhancement`, plus `user report`;
- adds the SandFly log, when included, as the first comment;
- breaks `@name` mentions in the reporter's text so that they notify nobody.

Responses: 201 sent, 400 bad field, 413 too large, 415 not JSON, 429 rate limited, 502 GitHub
error.

### Conversations

Each report gets a secret token, returned as `{"sent": true, "token": "…"}`. SandFly keeps it;
the Worker keeps only its SHA-256 in the `THREADS` KV namespace, with the issue number. The issue
body starts with a hidden `<!-- sandfly-report: <id> -->` line naming the report.

- `POST /threads` with `{"tokens": [...]}` (at most 20) returns `{"threads": [...]}`: for each
  token, `status` (`open`, `completed`, `not_planned`, `duplicate`, or `unknown` for a token the
  Worker doesn't know) and `messages`, each with `id`, `from` (`developer` or `reporter`), `body`
  and `at`.
- `POST /reply` with `{"token": "...", "body": "..."}` (at most 10,000 characters) adds the
  reporter's reply as a comment marked `<!-- sandfly-user -->`. 30 replies a day from one address.

Which comments the reporter sees:

- Comments you write on the issue on GitHub are replies, shown in SandFly.
- A comment that starts with `[private]` stays on GitHub and is not shown.
- The log comment, marked `<!-- sandfly-log -->`, is not shown.
- Closing the issue sets the status the reporter sees: closed as completed, as not planned, or as
  a duplicate.

SandFly checks for replies hourly while it runs, so a reply reaches the reporter within about an
hour.

## Set up

1. Create the private repository `aaron-gh/SandFly-reports`, with the labels `bug`,
   `enhancement` and `user report`.
2. Create a fine-grained personal access token at GitHub > Settings > Developer settings:
   - Repository access: only `aaron-gh/SandFly-reports`
   - Permissions: Issues, read and write
3. With Node.js installed (`brew install node`), in this directory:

   ```sh
   npx wrangler login
   npx wrangler kv namespace create COUNTS   # put the printed id in wrangler.toml
   npx wrangler kv namespace create THREADS  # the same
   npx wrangler secret put GITHUB_TOKEN      # paste the token
   npx wrangler deploy
   ```

4. Set `REPORT_URL` in `ProblemReportSender.kt` to the deployed address followed by `/report`
   (now `https://sandfly-reports.aaron-graham-hewitt.workers.dev/report`).

When the token expires, create a new one and run `npx wrangler secret put GITHUB_TOKEN` again.

## Test

```sh
/System/Library/Frameworks/JavaScriptCore.framework/Versions/Current/Helpers/jsc -m test/report_test.mjs
/System/Library/Frameworks/JavaScriptCore.framework/Versions/Current/Helpers/jsc -m test/thread_test.mjs
```

The test covers validation and the issue text in `src/report.js`. To run the whole Worker
locally against a stand-in for GitHub's API listening on port 8799:

```sh
npx wrangler dev --var GITHUB_API:http://127.0.0.1:8799 --var GITHUB_TOKEN:test
```
