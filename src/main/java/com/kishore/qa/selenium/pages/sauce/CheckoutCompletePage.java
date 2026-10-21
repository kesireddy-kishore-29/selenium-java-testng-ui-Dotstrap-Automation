package com.kishore.qa.selenium.pages.sauce;

import com.kishore.qa.selenium.pages.Accessible;
import com.kishore.qa.selenium.pages.BasePage;
import com.kishore.qa.selenium.pages.Locators;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Checkout complete ({@code /checkout-complete.html}). */
public final class CheckoutCompletePage extends BasePage {

  private static final By HEADER = Locators.testId("complete-header");
  private static final By BACK_HOME = Locators.testId("back-to-products");

  public CheckoutCompletePage(WebDriver driver) {
    super(driver);
  }

  CheckoutCompletePage awaitLoaded() {
    wait.urlContains("/checkout-complete.html");
    wait.visible(HEADER);
    return this;
  }

  /** The confirmation heading as a screen reader announces it. */
  public Accessible confirmation() {
    return Accessible.of(wait.visible(HEADER));
  }

  public InventoryPage backHome() {
    click(BACK_HOME);
    wait.urlContains("/inventory.html");
    return new InventoryPage(driver).awaitLoaded();
  }
}
