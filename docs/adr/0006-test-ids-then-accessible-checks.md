# 6. Locators: test ids first, then id or name; roles and names are checked, not searched

- Status: accepted
- Date: 2026-10-21

## Context

The UI flagship finds elements by ARIA role and accessible name first. Selenium has no role
locator: finding "the button called Login" needs XPath or a hand-written search. Both targets
here expose stable test ids (`data-test`) on almost every element a test touches.

## Decision

1. Find by `data-test` first, then by id or name, then by `aria-label`, then by tag inside an
   element already found. No CSS chains, no XPath. `Locators` is the only place a `By` is built.
2. Where it matters to a person, assert what assistive technology announces instead:
   `Accessible.of(element)` reads Chrome's computed role and name (`getAriaRole()`,
   `getAccessibleName()`). Tests check the Login control is a button named "Login", the
   confirmation is a heading, the menu button is named "Open Menu", each delete button is named
   after its row, and the confirm dialog is a dialog named "Delete item?".
3. Tests never see a locator or a `WebElement`; a search over `tests/` finds none.

## Consequences

- Where the UI flagship also uses a test id, it is the same one, so a renamed id breaks both
  repositories in the same way. The flagship finds buttons and fields by role and label instead;
  this repository depends on more test ids than it does.
- Sauce Demo's menu entries have no link role (no `href`); their test ids are the only handle. A
  finding, recorded in the [known issues](../known-issues.md).
- Full policy: [docs/locator-policy.md](../locator-policy.md).
