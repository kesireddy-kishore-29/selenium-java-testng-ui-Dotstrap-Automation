package com.qaautomation.selenium.pages.sauce;

import com.qaautomation.selenium.components.SauceHeader;
import com.qaautomation.selenium.data.Money;
import com.qaautomation.selenium.data.Product;
import com.qaautomation.selenium.pages.BasePage;
import com.qaautomation.selenium.pages.Locators;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Sauce Demo product list ({@code /inventory.html}). */
public final class InventoryPage extends BasePage {

  /** The sort menu's options, by their visible labels. */
  public enum Sort {
    NAME_A_TO_Z("Name (A to Z)"),
    NAME_Z_TO_A("Name (Z to A)"),
    PRICE_LOW_TO_HIGH("Price (low to high)"),
    PRICE_HIGH_TO_LOW("Price (high to low)");

    private final String label;

    Sort(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private static final By TITLE = Locators.testId("title");
  private static final By ITEM = Locators.testId("inventory-item");
  private static final By ITEM_NAME = Locators.testId("inventory-item-name");
  private static final By ITEM_PRICE = Locators.testId("inventory-item-price");
  private static final By SORT = Locators.testId("product-sort-container");

  private final SauceHeader header;

  public InventoryPage(WebDriver driver) {
    super(driver);
    this.header = new SauceHeader(driver, wait);
  }

  /** Opens the inventory directly; the browser must already hold a session. */
  public InventoryPage open() {
    driver.get(url(config.sauceBaseUrl(), "/inventory.html"));
    return awaitLoaded();
  }

  InventoryPage awaitLoaded() {
    wait.text(TITLE, "Products");
    wait.allVisible(ITEM);
    return this;
  }

  public SauceHeader header() {
    return header;
  }

  public String title() {
    return text(TITLE);
  }

  public int itemCount() {
    return driver.findElements(ITEM).size();
  }

  public List<String> names() {
    return driver.findElements(ITEM_NAME).stream().map(e -> e.getText().strip()).toList();
  }

  public List<BigDecimal> prices() {
    return driver.findElements(ITEM_PRICE).stream().map(e -> Money.parse(e.getText())).toList();
  }

  public InventoryPage sortBy(Sort sort) {
    new Select(wait.visible(SORT)).selectByVisibleText(sort.label());
    wait.until(
        "sort menu to show " + sort,
        d ->
            sort.label()
                .equals(new Select(d.findElement(SORT)).getFirstSelectedOption().getText()));
    return this;
  }

  public InventoryPage addToCart(Product product) {
    int before = header.cartCount();
    pressButton(product, "Add to cart");
    header.awaitCartCount(before + 1);
    return this;
  }

  public InventoryPage removeFromCart(Product product) {
    int before = header.cartCount();
    pressButton(product, "Remove");
    header.awaitCartCount(before - 1);
    return this;
  }

  public ProductDetailPage openDetails(Product product) {
    WebElement name =
        wait.until(
            "a product called '" + product.displayName() + "'",
            d ->
                card(product).findElements(ITEM_NAME).stream()
                    .filter(WebElement::isDisplayed)
                    .findFirst()
                    .orElse(null));
    name.click();
    return new ProductDetailPage(driver).awaitLoaded(product);
  }

  public CartPage openCart() {
    header.openCart();
    return new CartPage(driver).awaitLoaded();
  }

  /** Logs out through the menu; ends on the sign-in page. */
  public LoginPage logout() {
    header.logout();
    return new LoginPage(driver).awaitShown();
  }

  /** The product's card: the inventory item whose name is exactly the product's name. */
  private WebElement card(Product product) {
    return wait.until(
        "the card of '" + product.displayName() + "'",
        d ->
            d.findElements(ITEM).stream()
                .filter(c -> product.displayName().equals(c.findElement(ITEM_NAME).getText()))
                .findFirst()
                .orElse(null));
  }

  /** Each card has exactly one button; its label says what it will do. */
  private void pressButton(Product product, String label) {
    WebElement button = card(product).findElement(Locators.tag("button"));
    if (!label.equals(button.getText().strip())) {
      throw new IllegalStateException(
          "Expected '" + label + "' on " + product.displayName() + ", found '" + button.getText());
    }
    button.click();
  }
}
