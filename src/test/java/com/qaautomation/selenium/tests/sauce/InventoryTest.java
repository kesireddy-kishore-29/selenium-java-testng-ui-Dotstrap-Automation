package com.qaautomation.selenium.tests.sauce;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaautomation.selenium.annotations.Feature;
import com.qaautomation.selenium.annotations.UiTest;
import com.qaautomation.selenium.core.DriverFactory;
import com.qaautomation.selenium.data.Product;
import com.qaautomation.selenium.pages.sauce.InventoryPage;
import com.qaautomation.selenium.pages.sauce.InventoryPage.Sort;
import com.qaautomation.selenium.pages.sauce.ProductDetailPage;
import com.qaautomation.selenium.pages.sauce.SauceSession;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Sauce Demo product list: contents, sorting and the detail page. */
@UiTest
public final class InventoryTest {

  @Test(
      groups = {"smoke", "regression"},
      description = "the inventory lists every product with a name and a price")
  @Feature("FEAT-INV-01")
  public void listsEveryProductWithNameAndPrice() {
    InventoryPage inventory = SauceSession.signedIn(DriverFactory.driver());

    assertThat(inventory.itemCount()).isEqualTo(Product.values().length);
    assertThat(inventory.names())
        .containsExactlyInAnyOrderElementsOf(
            Arrays.stream(Product.values()).map(Product::displayName).toList());
    assertThat(inventory.prices())
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyInAnyOrderElementsOf(
            Arrays.stream(Product.values()).map(Product::price).toList());
  }

  @DataProvider(name = "sorts", parallel = true)
  public Object[][] sorts() {
    return Arrays.stream(Sort.values()).map(s -> new Object[] {s}).toArray(Object[][]::new);
  }

  @Test(dataProvider = "sorts", groups = "regression", description = "sorting orders the products")
  @Feature("FEAT-INV-02")
  public void sortingOrdersTheProducts(Sort sort) {
    InventoryPage inventory = SauceSession.signedIn(DriverFactory.driver()).sortBy(sort);

    switch (sort) {
      case NAME_A_TO_Z -> assertThat(inventory.names()).isSorted();
      case NAME_Z_TO_A ->
          assertThat(inventory.names()).isSortedAccordingTo(Comparator.reverseOrder());
      case PRICE_LOW_TO_HIGH -> assertThat(inventory.prices()).isSortedAccordingTo(byAmount());
      case PRICE_HIGH_TO_LOW ->
          assertThat(inventory.prices()).isSortedAccordingTo(byAmount().reversed());
      default -> throw new IllegalArgumentException("Unknown sort " + sort);
    }
    // Sorting reorders; it never drops or duplicates a product.
    assertThat(inventory.names())
        .containsExactlyInAnyOrderElementsOf(
            Arrays.stream(Product.values()).map(Product::displayName).toList());
  }

  @Test(
      groups = "regression",
      description = "a product detail page opens from the inventory and returns")
  @Feature("FEAT-INV-03")
  public void productDetailOpensAndReturns() {
    ProductDetailPage detail =
        SauceSession.signedIn(DriverFactory.driver()).openDetails(Product.FLEECE_JACKET);

    assertThat(detail.name()).isEqualTo(Product.FLEECE_JACKET.displayName());
    assertThat(detail.price()).isEqualByComparingTo(Product.FLEECE_JACKET.price());
    assertThat(detail.description()).isNotBlank();
    assertThat(detail.backToProducts().currentPath()).isEqualTo("/inventory.html");
  }

  private static Comparator<BigDecimal> byAmount() {
    return BigDecimal::compareTo;
  }
}
