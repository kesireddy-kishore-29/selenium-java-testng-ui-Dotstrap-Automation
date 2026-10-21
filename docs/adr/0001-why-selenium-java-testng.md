# 1. Why Selenium 4 with Java and TestNG in 2026, in a repository of its own

- Status: accepted
- Date: 2026-10-21

## Context

These repositories are meant to show range to people hiring for QA and SDET roles. The author
analysed about 980 recent QA/SDET job descriptions (Naukri and Foundit):

- Selenium with Java is the most common UI stack (46 to 61% of jobs). Playwright is the fastest
  growing, usually with TypeScript, and has its own flagship repository.
- Java is the most requested language for test automation; Python comes next (33 to 42%).
- TestNG is the runner named most often next to Selenium in those descriptions.

The author's day job is C# with Selenium. A Java Selenium repository shows the same skills in the
language most of those jobs ask for, and it shows what a Selenium framework must build itself that
Playwright gives for free: waits, a browser per thread, video, logs.

## Decision

1. This repository uses Selenium 4.51, Java 21, TestNG 7.12, AssertJ and Allure, built with
   Maven.
2. It is a lite repository: the Sauce Demo journeys of the UI flagship with the same feature IDs,
   one hybrid test against the UI flagship's self-hosted app, and a small live smoke. Triage,
   healing and the dashboard stay in the flagships; Allure categories give a triage preview here.
3. It shares the family's conventions: feature IDs, `RECORD_MODE`, the evidence layout, the
   `run-summary.json` and `flaky-report.json` formats and the gallery page (byte for byte).

## Alternatives considered

- **Selenium inside the UI flagship**: one repository with two UI stacks reads as neither being
  idiomatic.
- **JUnit 5**: equally capable. TestNG's suite files, groups, data providers and listener model are
  what Selenium job descriptions name, and the API flagship already uses it.
- **Selenide or Serenity**: they hide exactly the parts (waits, driver lifecycle) this repository
  exists to show.

## Consequences

- Waits, the browser lifecycle and video are written here, so they are tested here
  ([ADR 2](0002-thread-local-browser.md), [ADR 3](0003-explicit-waits-only.md),
  [ADR 4](0004-video-stitch-or-grid.md)).
- The shared formats are kept in step by hand; the gallery template is checked with `md5sum`
  against the flagships after every change.
