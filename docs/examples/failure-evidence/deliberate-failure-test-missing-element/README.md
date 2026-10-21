# Failure bundle: demo: a renamed test id is a missing element

| | |
|---|---|
| Test | `com.kishore.qa.selenium.tests.demo.DeliberateFailureTest#missingElement` |
| Features | FEAT-DEMO-01 |
| Run | `sel-demo-4` (git `5b1509b32eb7`) |
| Attempts | 2 |
| Failing locator | `By.cssSelector: [data-test="cart-count"]` |
| Error | `org.openqa.selenium.NoSuchElementException` |

## What is in here

| File | What it is | How to open |
|---|---|---|
| `attempt-N/video.webm` | The session, stitched from a screenshot after every page action (Selenium has no recorder) | Any browser or video player |
| `attempt-N/screenshot.png` | The viewport when the test failed | An image viewer |
| `attempt-N/fullpage.png` | The whole page when the test failed | An image viewer |
| `attempt-N/page-source.html` | The DOM when the test failed (redacted) | A browser or editor |
| `attempt-N/console.log` | The browser console (redacted) | A text editor |
| `attempt-N/network-log.json` | Every request: method, URL, status, failure (no headers) | A JSON viewer |
| `attempt-N/meta.json` | Test identity, features, browser, environment, git SHA, notes | A JSON viewer |

Selenium has no trace viewer; the page source, console and network log replace it.
