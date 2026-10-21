package com.kishore.qa.selenium.pages.app;

import com.kishore.qa.selenium.components.DataTable;
import com.kishore.qa.selenium.components.Modal;
import com.kishore.qa.selenium.pages.Accessible;
import com.kishore.qa.selenium.pages.BasePage;
import com.kishore.qa.selenium.pages.Locators;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * The UI flagship app's items page ({@code /items}): a data table with a confirm-to-delete dialog.
 * Same test ids and accessible names as the flagship's page object.
 */
public final class ItemsPage extends BasePage {

  private static final String SESSION_COOKIE = "sutui_session";
  private static final By TABLE = Locators.testId("items-table");
  private static final By CONFIRM_DIALOG = Locators.id("confirm");

  private final DataTable table;
  private final Modal confirmDelete;

  public ItemsPage(WebDriver driver) {
    super(driver);
    this.table = new DataTable(driver, wait, TABLE);
    this.confirmDelete = new Modal(wait, CONFIRM_DIALOG, "Delete item?");
  }

  /**
   * Opens the page signed in as the token's owner: the session cookie is set directly (the saved
   * session pattern), so no test clicks through the app's sign-in form.
   */
  public ItemsPage openSignedIn(String token) {
    // A cookie can only be set for the site the browser is on; the health check is the cheapest
    // page on it.
    driver.get(url(config.appBaseUrl(), "/api/health"));
    driver
        .manage()
        .addCookie(
            new Cookie.Builder(SESSION_COOKIE, token)
                .path("/")
                .isHttpOnly(true)
                .sameSite("Lax")
                .build());
    driver.get(url(config.appBaseUrl(), "/items"));
    table.element();
    return this;
  }

  public DataTable table() {
    return table;
  }

  public boolean hasItem(String name) {
    return table.row(name).isPresent();
  }

  /** The row's delete button as a screen reader announces it. */
  public Accessible deleteControl(String name) {
    return Accessible.of(deleteButton(name));
  }

  /** Deletes through the confirm dialog and waits until the row has gone. */
  public ItemsPage delete(String name) {
    deleteButton(name).click();
    confirmDelete.awaitOpen();
    confirmDelete.confirm("Delete");
    table.awaitNoRow(name);
    return this;
  }

  /** Opens the dialog and cancels it; the row must stay. */
  public ItemsPage cancelDelete(String name) {
    deleteButton(name).click();
    confirmDelete.awaitOpen();
    confirmDelete.cancel();
    table.awaitRow(name);
    return this;
  }

  private WebElement deleteButton(String name) {
    return table.awaitRow(name).findElement(Locators.ariaLabel("Delete " + name));
  }
}
