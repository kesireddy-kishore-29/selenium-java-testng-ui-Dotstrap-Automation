package com.kishore.qa.selenium.pages;

import org.openqa.selenium.By;

/**
 * The only ways a page object may find an element, in the order of docs/locator-policy.md: the
 * site's test id ({@code data-test}), then id or name, then an accessible label, then a tag inside
 * an element already found. No CSS chains and no XPath. {@link #metadata} reads document metadata
 * and is never used for an element a test interacts with.
 */
public final class Locators {

  private Locators() {}

  /** Rank 1: {@code [data-test="..."]}, the attribute both Sauce Demo and the sut-ui expose. */
  public static By testId(String testId) {
    return By.cssSelector("[data-test=" + quote(testId) + "]");
  }

  /** Rank 2: a stable id, where the element has no test id. */
  public static By id(String id) {
    return By.id(id);
  }

  /** Rank 2: a form control's name. */
  public static By name(String name) {
    return By.name(name);
  }

  /** Rank 3: an element whose accessible name is set with {@code aria-label}. */
  public static By ariaLabel(String label) {
    return By.cssSelector("[aria-label=" + quote(label) + "]");
  }

  /** Rank 4: a tag, only inside an element already found (a cell in a row, a button in a card). */
  public static By tag(String tag) {
    return By.tagName(tag);
  }

  /** Document metadata only ({@code meta[name]}, {@code link[rel]}): it has no role or name. */
  public static By metadata(String tag, String attribute, String value) {
    if (!"meta".equals(tag) && !"link".equals(tag)) {
      throw new IllegalArgumentException("metadata() is for <meta> and <link> only, not " + tag);
    }
    return By.cssSelector(tag + "[" + attribute + "=" + quote(value) + "]");
  }

  static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
