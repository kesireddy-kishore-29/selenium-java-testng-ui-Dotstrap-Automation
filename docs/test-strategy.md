# Test strategy

What this repository tests, where, and the rules every test follows. It is the lite Selenium
member of a family of four; the flagships carry the wider strategy (contract, resilience, visual,
accessibility, triage, healing).

## Targets

| Target | What only it can give | Gating? |
|---|---|---|
| [Sauce Demo](https://www.saucedemo.com) (public practice shop) | A real third-party UI: sign-in, product list, cart, checkout | Smoke gates pull requests; regression runs nightly |
| The UI flagship's self-hosted app (`sut-ui`, port 4173) | Data we own: seed through its API, check the browser, delete and prove it gone | Nightly, when the app is checked out; skipped otherwise |
| [dotstrap.com](https://dotstrap.com) public pages | The author's production site, read the way a visitor's browser reads it | Never ([ADR 5](adr/0005-live-smoke-policy.md)) |

## Suites

| Suite (`-Dsuite=`) | Contents | Runs |
|---|---|---|
| `unit` | 54 framework unit tests, no browser | Every push (CI `quality`) |
| `smoke` | 3 journeys: sign in, product list, full checkout | Every push and pull request |
| `regression` | 12 test methods, 20 runs with data providers, including the hybrid test | Nightly, retries on |
| `live` | 4 checks, 17 runs, one browser | Nightly and on demand, never gating |
| `demo-failure` | 3 tests that fail on purpose | On demand, to show the evidence |
| `quarantine` | Tests in the `quarantine` group (none today) | Nightly when there are any, never gating |

## Rules every browser test follows

- **One browser per test**, started and quit by a listener; no shared driver, no shared data
  ([ADR 2](adr/0002-thread-local-browser.md)).
- **Explicit waits only**; Checkstyle refuses `Thread.sleep` and implicit waits
  ([ADR 3](adr/0003-explicit-waits-only.md)).
- **No order dependence**: Checkstyle refuses `dependsOnMethods`/`dependsOnGroups`. A test that
  needs a signed-in shop seeds the session cookie itself (`SauceSession`).
- **Own data, cleaned and verified**: the hybrid test creates its own account and items through
  the API; `AppAccount.close()` deletes them, checks they are gone and that the account can no
  longer sign in. A cleanup that fails fails the test.
- **Traceable**: every browser test has `@Feature("FEAT-…")`; a suite containing one without it
  does not start (`TraceabilityListener`). The IDs are shared with the other repositories.
- **No locators in tests**: tests talk to page objects ([locator policy](locator-policy.md)).
- **No disabled tests**: Checkstyle refuses `enabled = false`; a test that cannot be trusted goes to
  quarantine instead.

## Flaky tests and quarantine

- A failed browser test is retried at most twice (`TEST_RETRIES` lowers it; the demo retries once).
  A test that passes on a retry is reported as **flaky** in `run-summary.json` and
  `flaky-report.json`, never as a plain pass. Unit tests are never retried.
- A test that keeps flaking goes into the `quarantine` group **and** gets an entry in
  `quarantine.json` with an owner, a reason, an expiry date and an issue. A unit test fails the
  build when an entry has expired or has no owner, or when the group and the file disagree.
  Quarantined tests still run, in their own non-gating suite.

## Evidence

`RECORD_MODE` decides what each test keeps: `failures` (default), `all` (nightly), `off`. Kept per
attempt: `video.webm`, `screenshot.png`, `fullpage.png`, `page-source.html`, `console.log`,
`network-log.json`, `meta.json`. Per run: `run-summary.json`, `flaky-report.json`, `gallery.html`,
`summary.md` and a failure bundle per failed test. Every text file is redacted (credential headers,
cookies, tokens, passwords).

## Environments

`-Plocal` (default) and `-Pci` choose `config/local.properties` or `config/ci.properties`. Any key
can be overridden by an environment variable (`sauce.baseUrl` → `SAUCE_BASE_URL`) or a system
property; every value is validated at start-up and all problems are reported at once.
