# SandFly report server

A Cloudflare Worker that receives the reports from Settings > Report a problem and creates
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
   npx wrangler secret put GITHUB_TOKEN      # paste the token
   npx wrangler deploy
   ```

4. Set `REPORT_URL` in `ProblemReportSender.kt` to the deployed address followed by `/report`
   (now `https://sandfly-reports.aaron-graham-hewitt.workers.dev/report`).

When the token expires, create a new one and run `npx wrangler secret put GITHUB_TOKEN` again.

## Test

```sh
/System/Library/Frameworks/JavaScriptCore.framework/Versions/Current/Helpers/jsc -m test/report_test.mjs
```

The test covers validation and the issue text in `src/report.js`. To run the whole Worker
locally against a stand-in for GitHub's API listening on port 8799:

```sh
npx wrangler dev --var GITHUB_API:http://127.0.0.1:8799 --var GITHUB_TOKEN:test
```
