package com.qaautomation.selenium.pages.sauce;

import com.qaautomation.selenium.components.SauceHeader;
import com.qaautomation.selenium.pages.BasePage;
import com.qaautomation.selenium.pages.Locators;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Sauce Demo cart ({@code /cart.html}). */
public final class CartPage extends BasePage {

  private static final By TITLE = Locators.testId("title");
  private static final By ITEM_NAME = Locators.testId("inventory-item-name");
  private static final By CHECKOUT = Locators.testId("checkout");
  private static final By CONTINUE_SHOPPING = Locators.testId("continue-shopping");

  private final SauceHeader header;

  public CartPage(WebDriver driver) {
    super(driver);
    this.header = new SauceHeader(driver, wait);
  }

  CartPage awaitLoaded() {
    wait.urlContains("/cart.html");
    wait.text(TITLE, "Your Cart");
    return this;
  }

  public SauceHeader header() {
    return header;
  }

  public List<String> names() {
    return driver.findElements(ITEM_NAME).stream().map(e -> e.getText().strip()).toList();
  }

  public CheckoutInformationPage checkout() {
    click(CHECKOUT);
    return new CheckoutInformationPage(driver).awaitLoaded();
  }

  public InventoryPage continueShopping() {
    click(CONTINUE_SHOPPING);
    wait.urlContains("/inventory.html");
    return new InventoryPage(driver).awaitLoaded();
  }
}
