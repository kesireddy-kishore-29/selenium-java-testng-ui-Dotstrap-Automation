package com.kishore.qa.selenium.tests.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.kishore.qa.selenium.annotations.Feature;
import com.kishore.qa.selenium.annotations.UiTest;
import com.kishore.qa.selenium.core.DriverFactory;
import com.kishore.qa.selenium.data.Product;
import com.kishore.qa.selenium.pages.demo.DemoPage;
import com.kishore.qa.selenium.pages.sauce.InventoryPage;
import com.kishore.qa.selenium.pages.sauce.SauceSession;
import org.testng.annotations.Test;

/**
 * Three tests that fail on purpose, one per kind of failure a UI suite meets, to prove what
 * evidence each leaves behind (video, screenshots, page source, logs, bundle). Group {@code
 * demo-failure}: never part of a gating run, retried once to show a retry in the evidence.
 */
@UiTest
public final class DeliberateFailureTest {

  @Test(groups = "demo-failure", description = "demo: a wrong expectation fails an assertion")
  @Feature("FEAT-DEMO-01")
  public void wrongExpectation() {
    InventoryPage inventory = SauceSession.signedIn(DriverFactory.driver());

    assertThat(inventory.title()).as("inventory title").isEqualTo("Product catalogue");
  }

  @Test(groups = "demo-failure", description = "demo: a renamed test id is a missing element")
  @Feature("FEAT-DEMO-01")
  public void missingElement() {
    SauceSession.signedIn(DriverFactory.driver()).addToCart(Product.BACKPACK);

    assertThat(new DemoPage(DriverFactory.driver()).renamedCartBadge()).isEqualTo("1");
  }

  @Test(groups = "demo-failure", description = "demo: waiting for a page that never comes")
  @Feature("FEAT-DEMO-01")
  public void timeout() {
    SauceSession.signedIn(DriverFactory.driver());

    new DemoPage(DriverFactory.driver()).awaitOrderConfirmation();
  }
}
