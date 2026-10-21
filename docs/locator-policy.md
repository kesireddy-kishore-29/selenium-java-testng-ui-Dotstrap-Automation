# Locator policy

A locator is an assertion about how a person finds something on the page. The best one fails only
when a person would also be lost, and survives every change that a person would not notice.

Selenium has no locator for "the button called Login" (Playwright's `getByRole`). Rather than
reach for XPath, this repository finds elements by the stable hooks both targets publish, then
**checks** the role and name a screen reader announces where that matters to a person.

## Order of preference

| Rank | Locator (`Locators.*`) | Use it for | Example |
|---|---|---|---|
| 1 | `testId` (`[data-test="…"]`) | Anything the site gives a test id; both targets expose them widely | `Locators.testId("shopping-cart-badge")` |
| 2 | `id`, `name` | An element with a stable id and no test id | `Locators.id("react-burger-menu-btn")` |
| 3 | `ariaLabel` | An element whose accessible name is set with `aria-label` | `Locators.ariaLabel("Delete " + name)` |
| 4 | `tag` | A tag inside an element already found: a cell in a row, the one button in a product card. One page-level use: the page's `h1` | `row.findElement(Locators.tag("td"))` |
| — | `metadata` | `<meta>` and `<link>` only (description, canonical); never an element a test touches | `Locators.metadata("link", "rel", "canonical")` |
| — | CSS chains, XPath | Never | |

In `src/main/java/.../pages` and `components` today: 34 test ids (32 of them on Sauce Demo,
1 on the self-hosted app's table, 1 deliberately wrong one in the demo page), 2 ids, 1 aria-label,
9 tag lookups and 2 metadata reads. `Locators` is the only class that builds a `By`.

## Rules

- **Locators live in page objects and components.** A test reads `inventory.addToCart(BACKPACK)`;
  it never sees a `By` or a `WebElement` (checked with a search over `src/test/java/.../tests`).
- **Check what a person hears.** `Accessible.of(element)` returns Chrome's computed role and
  accessible name (`getAriaRole()`, `getAccessibleName()`). Tests assert that the Login control is
  a `button` named "Login", the order confirmation is a `heading`, the menu button is named
  "Open Menu", a row's delete button is named after the row, and the confirm dialog is a `dialog`
  named "Delete item?" (the `Modal` component refuses anything else).
- **Scope instead of index.** A product card is found by its exact name, then its one button is
  pressed, and the button's label is checked first ("Add to cart" or "Remove"). A table cell is
  found under the column whose header has that name (`DataTable`), never by a fixed index.
- **Find again after a change.** Both targets re-render after every action. Components keep a `By`,
  not a `WebElement`, and waits retry through stale elements.
- **Wait for the state, not for time.** Page objects return the next page only once it is there
  ([ADR 3](adr/0003-explicit-waits-only.md)).

## When a third party gets in the way

Sauce Demo's menu entries are `<a>` elements without `href`, so they have no link role; the page
object uses their test ids, and a comment says why. The burger button has no test id; its id is
used. Both are findings against the site ([known issues](known-issues.md)), not licences to skip a
rank for convenience.

## Compared with the UI flagship

The UI flagship finds buttons, inputs, headings, tables and dialogs by role, label or placeholder,
and uses test ids for what has no role (titles, prices, the cart badge, the menu entries). This
repository uses test ids for nearly everything and checks roles afterwards. Where both repositories
use a test id (the product list, names, prices, sort menu, cart link and badge, logout entry, order
totals, error message) it is the same one, so a site renaming it breaks both the same way; the UI
flagship's self-healing demo shows such a rename being detected and healed.
