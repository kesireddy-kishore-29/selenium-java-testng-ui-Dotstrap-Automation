package com.kishore.qa.selenium.pages.sauce;

import com.kishore.qa.selenium.data.Money;
import com.kishore.qa.selenium.data.Product;
import com.kishore.qa.selenium.pages.BasePage;
import com.kishore.qa.selenium.pages.Locators;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Sauce Demo product detail ({@code /inventory-item.html?id=N}). */
public final class ProductDetailPage extends BasePage {

  private static final By NAME = Locators.testId("inventory-item-name");
  private static final By PRICE = Locators.testId("inventory-item-price");
  private static final By DESCRIPTION = Locators.testId("inventory-item-desc");
  private static final By BACK = Locators.testId("back-to-products");

  public ProductDetailPage(WebDriver driver) {
    super(driver);
  }

  ProductDetailPage awaitLoaded(Product product) {
    wait.urlContains("/inventory-item.html");
    wait.text(NAME, product.displayName());
    return this;
  }

  public String name() {
    return text(NAME);
  }

  public BigDecimal price() {
    return Money.parse(text(PRICE));
  }

  public String description() {
    return text(DESCRIPTION);
  }

  public InventoryPage backToProducts() {
    click(BACK);
    wait.urlContains("/inventory.html");
    return new InventoryPage(driver).awaitLoaded();
  }
}
