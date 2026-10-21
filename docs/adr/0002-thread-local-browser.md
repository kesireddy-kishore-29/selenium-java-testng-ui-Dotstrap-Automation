# 2. One browser per test, held in a ThreadLocal and owned by a listener

- Status: accepted
- Date: 2026-10-21

## Context

TestNG runs methods (and data-provider rows) in parallel on a thread pool. A `WebDriver` is not
thread-safe, and a static driver shared by tests is the classic cause of tests typing into each
other's browsers. Tests also must not depend on each other's state (a cart left full, a cookie
left behind).

## Decision

1. Every browser test gets a new Chrome, started before the method and quit after it.
2. The driver lives in a `ThreadLocal` in `DriverFactory`; page objects receive the driver of the
   test that created them. Nothing else holds a driver.
3. A listener (`EvidenceListener`) starts and quits the browser, not a base class and not the test.
   A test class only says `@UiTest`. Evidence is captured between the end of the test and `quit()`,
   while the browser is still open.
4. Checkstyle refuses mutable static fields, so no test can share a driver or data by accident.

## Alternatives considered

- **One browser per class or per suite**: faster, but state leaks between tests and a retried
  test would not start clean.
- **A base test class with `@BeforeMethod`/`@AfterMethod`**: works, but every test class must
  extend it, and evidence capture would race TestNG's own result reporting.

## Consequences

- Every test pays for a browser start. Three run at a time (the suite files set `thread-count`);
  the regression suite took 41 to 49 s for 20 tests in the local runs.
- A test that skips before its body (a failed configuration method) is still recorded as skipped
  in `run-summary.json`, because the listener records tests that never got a browser.
