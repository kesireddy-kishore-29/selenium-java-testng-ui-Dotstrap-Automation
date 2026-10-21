# Known issues and limits

Everything below is stated as it is. Where something was not run, it says so.

## Not verified yet

| What | State |
|---|---|
| **GitHub Actions** | `ci.yml` and `nightly.yml` pass actionlint 1.7.12 with shellcheck 0.11.0 but have **never run on GitHub**: the repository had no remote while it was built. Not yet exercised: installing ffmpeg on the runner, Chrome and the driver on Linux, artifact hand-off to the publish job, GitHub Pages, the live badge, checking out the UI flagship's app. |
| **Selenium Grid video** (`VIDEO_MODE=grid`, nightly job `grid-video`, `docker/selenium-grid.yml`) | **NOT RUN.** Docker could not run on the build machine (WSL is not installed; installing it needs administrator rights and a reboot). The compose file uses `latest` image tags, not yet pinned, and has never been started. The evidence code that finds a test's Grid video by its session ID has never seen a real Grid video. Every video in this repository comes from the default frame-stitching mode. |
| **Hybrid test in CI** | Runs only when the repository variables `FLAGSHIP_OWNER` and `UI_FLAGSHIP_REF` point at the UI flagship; otherwise it is skipped. Verified locally against the flagship's app on port 4173 only. |
| **Browsers** | Chrome only (headless, 1366×900 window). Firefox and Edge are not wired in. |
| **Operating systems** | Run on Windows 11 only. |

## Behaviour to know about

- **Badges and links assume the GitHub account `kesireddy-kishore-29`.** Until the first workflow
  runs there, the badges show no status; nothing here claims a CI result.
- **The live site changes on its own schedule.** A content change on dotstrap.com can fail the live
  smoke with no change in this repository. The pages and their expected text are in one file,
  [`config/live-pages.json`](../src/main/resources/config/live-pages.json). The smoke never gates.
- **The stitched video is a replay of page states, not real time**: one frame per page action, so
  a 10-second wait is a single frame ([ADR 4](adr/0004-video-stitch-or-grid.md)).
- **`RECORD_MODE=all` is slower**: a screenshot after every action. The regression took 44 and
  49 s with it and 41 and 42 s without in the local runs.
- **Allure appends to `target/allure-results/`** across local runs. Delete it before a run to report
  that run alone; CI always starts clean.
- **Allure keeps Selenium's full error text**, including its "Build info / Capabilities" block,
  which names the local Chrome profile folder. This repository's own files (`run-summary.json`,
  `meta.json`, bundles) leave that block out; Allure's copy is generated on the machine that ran
  the tests and published only from CI.
- **Selenium Manager** downloads the matching ChromeDriver on first use. On the build machine it
  once warned about an unreadable metadata cache and carried on.

## Findings against Sauce Demo

- Loading a deep link directly (for example `/inventory.html` after setting the session cookie)
  answers **HTTP 404** while the page still renders, and Chrome logs the 404 as a console error.
  Visible in every run's `network-log.json` and `console.log`.
- The menu entries are `<a>` elements **without `href`**, so they have no link role; the page
  object uses their test ids. The burger button has no test id; its id is used.
- The password field has no `autocomplete` attribute; Chrome logs a `[DOM]` suggestion about it
  on the sign-in page.

## Defects found in this framework while building it (all fixed)

- The failing locator was cut short: Chrome writes the selector inside unescaped quotes
  (`"selector":"[data-test="x"]"`), and the first parser stopped at the inner quote. Caught by the
  deliberate-failure demo; a unit test now uses the real message.
- Selenium appends build, driver and capability details (including the local Chrome profile path)
  to every error; they are now stripped from messages and stacks the framework writes.
- The overflow check resized the window, not the viewport: the viewport came out 18 px narrower,
  and Chrome refused a window narrower than about 500 px, so "390 px" was really 504 px. The test's
  own precondition ("the browser really is N px wide") caught it; the viewport is now set exactly
  through Chrome DevTools.
- Videos were padded with white bars (sized to the window, not the viewport); they now take the
  first frame's size.
- `mvnw` and the shell scripts were committed without the executable bit (written on Windows), so a
  Linux runner would have refused them. Found here and fixed in all three repositories built so
  far, before any CI run.
