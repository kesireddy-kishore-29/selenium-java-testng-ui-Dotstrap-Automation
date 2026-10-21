package com.qaautomation.selenium.pages.demo;

import com.qaautomation.selenium.pages.BasePage;
import com.qaautomation.selenium.pages.Locators;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Deliberately wrong expectations about Sauce Demo's inventory, used only by the deliberate-failure
 * demo (group {@code demo-failure}) to prove what evidence a real failure leaves behind. Nothing
 * else may use this class.
 */
public final class DemoPage extends BasePage {

  // A test id the page does not have: what a renamed attribute looks like to a test.
  private static final By RENAMED_BADGE = Locators.testId("cart-count");

  public DemoPage(WebDriver driver) {
    super(driver);
  }

  /** Looks the badge up by a test id that does not exist: NoSuchElementException. */
  public String renamedCartBadge() {
    return driver.findElement(RENAMED_BADGE).getText();
  }

  /** Waits for a page nothing navigates to: TimeoutException after the configured wait. */
  public void awaitOrderConfirmation() {
    wait.urlContains("/checkout-complete.html");
  }
}
