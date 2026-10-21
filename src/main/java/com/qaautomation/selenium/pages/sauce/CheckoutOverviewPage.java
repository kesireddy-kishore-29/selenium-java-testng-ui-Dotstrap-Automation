package com.qaautomation.selenium.pages.sauce;

import com.qaautomation.selenium.data.Money;
import com.qaautomation.selenium.pages.BasePage;
import com.qaautomation.selenium.pages.Locators;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Checkout step two: review ({@code /checkout-step-two.html}). */
public final class CheckoutOverviewPage extends BasePage {

  private static final By ITEM_PRICE = Locators.testId("inventory-item-price");
  private static final By ITEM_TOTAL = Locators.testId("subtotal-label");
  private static final By TAX = Locators.testId("tax-label");
  private static final By TOTAL = Locators.testId("total-label");
  private static final By FINISH = Locators.testId("finish");

  public CheckoutOverviewPage(WebDriver driver) {
    super(driver);
  }

  CheckoutOverviewPage awaitLoaded() {
    wait.urlContains("/checkout-step-two.html");
    wait.visible(TOTAL);
    return this;
  }

  public List<BigDecimal> itemPrices() {
    return driver.findElements(ITEM_PRICE).stream().map(e -> Money.parse(e.getText())).toList();
  }

  public BigDecimal itemTotal() {
    return Money.parse(text(ITEM_TOTAL));
  }

  public BigDecimal tax() {
    return Money.parse(text(TAX));
  }

  public BigDecimal total() {
    return Money.parse(text(TOTAL));
  }

  public CheckoutCompletePage finish() {
    click(FINISH);
    return new CheckoutCompletePage(driver).awaitLoaded();
  }
}
