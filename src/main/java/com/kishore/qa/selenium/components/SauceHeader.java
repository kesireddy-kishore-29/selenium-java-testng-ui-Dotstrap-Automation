package com.kishore.qa.selenium.components;

import com.kishore.qa.selenium.core.WaitHelper;
import com.kishore.qa.selenium.pages.Accessible;
import com.kishore.qa.selenium.pages.Locators;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Sauce Demo's header, on every signed-in page: the cart link with its badge, and the burger menu.
 * A component, not a page: pages hold one and expose it as {@code header()}.
 */
public final class SauceHeader {

  private static final By CART_LINK = Locators.testId("shopping-cart-link");
  private static final By CART_BADGE = Locators.testId("shopping-cart-badge");
  // The burger button has no test id; its id is the next rank in the locator policy.
  private static final By MENU_BUTTON = Locators.id("react-burger-menu-btn");
  // The menu entries are <a> without href, so they have no link role (a finding recorded in
  // docs/known-issues.md); their test ids are the only stable handle.
  private static final By LOGOUT = Locators.testId("logout-sidebar-link");

  private final WebDriver driver;
  private final WaitHelper wait;

  public SauceHeader(WebDriver driver, WaitHelper wait) {
    this.driver = driver;
    this.wait = wait;
  }

  /** The number on the cart badge; 0 when the badge is not shown (an empty cart). */
  public int cartCount() {
    List<WebElement> badge = driver.findElements(CART_BADGE);
    return badge.isEmpty() ? 0 : Integer.parseInt(badge.getFirst().getText().strip());
  }

  /** Waits until the badge shows {@code expected} (0: no badge). */
  public void awaitCartCount(int expected) {
    wait.until("cart badge to show " + expected, d -> cartCount() == expected);
  }

  public void openCart() {
    wait.clickable(CART_LINK).click();
  }

  /** Opens the menu (it slides in) and clicks Logout. */
  public void logout() {
    wait.clickable(MENU_BUTTON).click();
    wait.clickable(LOGOUT).click();
  }

  /** The menu button as a screen reader announces it. */
  public Accessible menuButton() {
    return Accessible.of(wait.visible(MENU_BUTTON));
  }
}
