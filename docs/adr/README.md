# Architecture decision records

One file per decision: context, decision, consequences (and alternatives where there were real
ones). A decision that changes gets a new record that supersedes the old one.

| # | Decision |
|---|---|
| [1](0001-why-selenium-java-testng.md) | Why Selenium 4 with Java and TestNG in 2026, in a repository of its own |
| [2](0002-thread-local-browser.md) | One browser per test, held in a ThreadLocal and owned by a listener |
| [3](0003-explicit-waits-only.md) | Explicit waits only |
| [4](0004-video-stitch-or-grid.md) | Video: frames stitched with ffmpeg by default, the Grid's recorder as an option |
| [5](0005-live-smoke-policy.md) | The live smoke is a polite, read-only visitor |
| [6](0006-test-ids-then-accessible-checks.md) | Locators: test ids first, then id or name; roles and names are checked, not searched |
