# 3. Explicit waits only

- Status: accepted
- Date: 2026-10-21

## Context

Selenium offers three ways to wait: an implicit wait on every `findElement`, explicit
`WebDriverWait` conditions, and `Thread.sleep`. Mixing the first two makes timeouts unpredictable
(an explicit wait for absence can take implicit-wait × polls), and sleeps are either too long or
too short. Sauce Demo and the self-hosted app both re-render after each action, so an element
found a moment ago can be stale.

## Decision

1. The implicit wait stays at zero. Checkstyle refuses `implicitlyWait(` and `Thread.sleep(`
   anywhere in the repository (proven with a planted file during the build).
2. `WaitHelper` is the only way to wait: visible, clickable, gone, URL contains, text equals, or
   any named condition. Every wait has one timeout from configuration (`wait.timeoutSeconds`) and
   says in its message what it was waiting for.
3. Waits retry through `StaleElementReferenceException`: a re-render between finding and reading
   an element is not a failure.
4. Page objects wait for the state they promise. `signInAs` returns an `InventoryPage` only after
   the URL changed and the product list is visible, so tests never wait themselves.

## Consequences

- A timeout message names the locator or condition (`waiting 10 s for visibility of
  By.cssSelector: [data-test="..."]`); the evidence listener extracts the failing locator from it
  and Allure's categories sort it as `LOCATOR_DRIFT` or `TIMING`.
- `findElements` (plural) is used for "is it there right now" questions, because it does not wait.
