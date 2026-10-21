# 5. The live smoke is a polite, read-only visitor

- Status: accepted
- Date: 2026-10-21

## Context

The author's own production site, dotstrap.com, is the one target here that real people use. A
status check alone can pass while a client-rendered page shows nothing, so the smoke reads the
rendered page. It must never load the site in a way its owner would mind.

## Decision

1. Public pages only: no sign-in (`/login` is refused by the configuration loader), no forms, no
   data created, no calls to the backend.
2. One browser at a time (`live.xml`: no parallelism), no retries in CI, and a user agent ending
   in ` selenium-java-testng-ui-Dotstrap-Automation (portfolio smoke test)`; the first check asserts it.
3. Nightly and on demand only, never on pull requests, and never gating: the job continues on
   error and its result feeds a badge.
4. The pages and the text they must show are in one file, `config/live-pages.json`.
5. Its evidence is a 7-day workflow artifact, never published to Pages and never committed. No
   HAR or trace is recorded at all.

## Consequences

- Four checks: status 200 with the expected heading (9 pages), no console errors or failed
  requests (all pages, one browser), title, description and canonical URL (3 pages), no horizontal
  overflow at 1440, 1024, 768 and 390 px (3 pages). 17 runs in about a minute.
- A content change on the site can fail the smoke with no change here; that is a one-file fix.
