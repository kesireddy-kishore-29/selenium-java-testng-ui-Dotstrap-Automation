package com.kishore.qa.selenium.components;

import com.kishore.qa.selenium.core.WaitHelper;
import com.kishore.qa.selenium.pages.Locators;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * A data table addressed by column header names and row content, never by a fixed column index or a
 * CSS path. The table is found again on every call, because the app re-renders it after each change
 * (a held {@link WebElement} would go stale).
 */
public final class DataTable {

  private final WebDriver driver;
  private final WaitHelper wait;
  private final By root;

  public DataTable(WebDriver driver, WaitHelper wait, By root) {
    this.driver = driver;
    this.wait = wait;
    this.root = root;
  }

  public WebElement element() {
    return wait.visible(root);
  }

  public List<String> headers() {
    WebElement head = element().findElement(Locators.tag("thead"));
    return head.findElements(Locators.tag("th")).stream().map(th -> th.getText().strip()).toList();
  }

  /** The body rows. */
  public List<WebElement> rows() {
    return element().findElement(Locators.tag("tbody")).findElements(Locators.tag("tr"));
  }

  /** The row with a cell whose text is exactly {@code text}. */
  public Optional<WebElement> row(String text) {
    if (driver.findElements(root).isEmpty()) {
      return Optional.empty();
    }
    return rows().stream()
        .filter(
            r ->
                r.findElements(Locators.tag("td")).stream().anyMatch(c -> text.equals(c.getText())))
        .findFirst();
  }

  public WebElement awaitRow(String text) {
    return wait.until("a row '" + text + "' in " + root, d -> row(text).orElse(null));
  }

  public void awaitNoRow(String text) {
    wait.until("no row '" + text + "' in " + root, d -> row(text).isEmpty());
  }

  /** The text under the column called {@code header} in the row holding {@code rowText}. */
  public String cell(String rowText, String header) {
    List<String> headers = headers();
    int index = headers.indexOf(header);
    if (index < 0) {
      throw new IllegalArgumentException("No column '" + header + "' in " + headers);
    }
    List<WebElement> cells = awaitRow(rowText).findElements(Locators.tag("td"));
    return cells.get(index).getText().strip();
  }
}
