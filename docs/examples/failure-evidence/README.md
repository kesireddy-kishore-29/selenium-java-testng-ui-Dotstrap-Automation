# Failure evidence: a renamed test id

`DeliberateFailureTest#missingElement` adds a product to the cart, then looks the cart badge up by
a test id the page does not have (`cart-count`; the real one is `shopping-cart-badge`). That is
what a renamed attribute looks like to a test. It failed, was retried once, and failed again.

The folder [`deliberate-failure-test-missing-element/`](deliberate-failure-test-missing-element/)
is the bundle exactly as the run wrote it (the zip next to it in the run folder is left out). Its
own README says what each file is.

| | |
|---|---|
| Run | `sel-demo-4`, git `5b1509b32eb7`, `RECORD_MODE=failures` |
| Error | `NoSuchElementException`; failing locator `By.cssSelector: [data-test="cart-count"]` |
| Attempts | 2 (the demo retries once); both kept |
| Video | `attempt-N/video.webm`: 4 frames at 2 per second (2 s), 1348×748, about 66 KB |
| Allure | the same failure lands in the `LOCATOR_DRIFT` category ([screenshot](../allure/categories.png), run `sel-demo-3`) |

Open `attempt-2/screenshot.png` to see the page at the moment of failure: the badge shows 1, so the
product really is in the cart; only the locator is wrong.
