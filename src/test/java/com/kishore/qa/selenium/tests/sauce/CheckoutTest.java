package com.kishore.qa.selenium.tests.sauce;

import static com.kishore.qa.selenium.data.Customer.aCustomer;
import static org.assertj.core.api.Assertions.assertThat;

import com.kishore.qa.selenium.annotations.Feature;
import com.kishore.qa.selenium.annotations.UiTest;
import com.kishore.qa.selenium.core.DriverFactory;
import com.kishore.qa.selenium.data.Customer;
import com.kishore.qa.selenium.data.Product;
import com.kishore.qa.selenium.flows.CheckoutFlow;
import com.kishore.qa.selenium.pages.Accessible;
import com.kishore.qa.selenium.pages.sauce.CheckoutCompletePage;
import com.kishore.qa.selenium.pages.sauce.CheckoutInformationPage;
import com.kishore.qa.selenium.pages.sauce.CheckoutOverviewPage;
import com.kishore.qa.selenium.pages.sauce.InventoryPage;
import com.kishore.qa.selenium.pages.sauce.SauceSession;
import java.math.BigDecimal;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Sauce Demo checkout: the full path, form validation and the order arithmetic. */
@UiTest
public final class CheckoutTest {

  @Test(
      groups = {"smoke", "regression"},
      description = "a full checkout completes with a confirmation")
  @Feature("FEAT-CHECKOUT-01")
  public void fullCheckoutCompletes() {
    InventoryPage inventory = SauceSession.signedIn(DriverFactory.driver());

    CheckoutCompletePage complete =
        CheckoutFlow.from(inventory).add(Product.BACKPACK).as(aCustomer()).finish();

    assertThat(complete.currentPath()).isEqualTo("/checkout-complete.html");
    assertThat(complete.confirmation())
        .isEqualTo(new Accessible("heading", "Thank you for your order!"));
    assertThat(complete.backHome().header().cartCount()).isZero();
  }

  @DataProvider(name = "incompleteCustomers", parallel = true)
  public Object[][] incompleteCustomers() {
    return new Object[][] {
      {aCustomer().without("first name"), "First Name is required"},
      {aCustomer().without("last name"), "Last Name is required"},
      {aCustomer().without("postal code"), "Postal Code is required"},
    };
  }

  @Test(
      dataProvider = "incompleteCustomers",
      groups = "regression",
      description = "checkout refuses a missing field with a specific message")
  @Feature("FEAT-CHECKOUT-02")
  public void checkoutRefusesMissingField(Customer customer, String expectedError) {
    CheckoutInformationPage information =
        CheckoutFlow.from(SauceSession.signedIn(DriverFactory.driver()))
            .add(Product.BIKE_LIGHT)
            .startCheckout()
            .fill(customer)
            .continueExpectingError();

    assertThat(information.error()).contains(expectedError);
    assertThat(information.currentPath()).isEqualTo("/checkout-step-one.html");
  }

  @Test(groups = "regression", description = "the order summary adds up: item total + tax = total")
  @Feature("FEAT-CHECKOUT-03")
  public void orderSummaryAddsUp() {
    List<Product> products = List.of(Product.BACKPACK, Product.FLEECE_JACKET, Product.ONESIE);

    CheckoutOverviewPage overview =
        CheckoutFlow.from(SauceSession.signedIn(DriverFactory.driver()))
            .add(products.toArray(Product[]::new))
            .as(aCustomer());

    BigDecimal expectedItemTotal =
        products.stream().map(Product::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(overview.itemPrices())
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyInAnyOrderElementsOf(products.stream().map(Product::price).toList());
    assertThat(overview.itemTotal()).isEqualByComparingTo(expectedItemTotal);
    assertThat(overview.tax()).isPositive();
    assertThat(overview.total()).isEqualByComparingTo(overview.itemTotal().add(overview.tax()));
  }
}
