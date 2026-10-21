package com.kishore.qa.selenium.tests.sauce;

import static org.assertj.core.api.Assertions.assertThat;

import com.kishore.qa.selenium.annotations.Feature;
import com.kishore.qa.selenium.annotations.UiTest;
import com.kishore.qa.selenium.core.DriverFactory;
import com.kishore.qa.selenium.data.Product;
import com.kishore.qa.selenium.pages.sauce.InventoryPage;
import com.kishore.qa.selenium.pages.sauce.SauceSession;
import org.testng.annotations.Test;

/** Sauce Demo cart badge. */
@UiTest
public final class CartTest {

  @Test(
      groups = "regression",
      description = "adding and removing items keeps the badge count right")
  @Feature("FEAT-CART-01")
  public void badgeFollowsAddAndRemove() {
    InventoryPage inventory = SauceSession.signedIn(DriverFactory.driver());
    assertThat(inventory.header().cartCount()).isZero();

    inventory.addToCart(Product.BACKPACK).addToCart(Product.BIKE_LIGHT);
    assertThat(inventory.header().cartCount()).isEqualTo(2);

    inventory.removeFromCart(Product.BACKPACK);
    assertThat(inventory.header().cartCount()).isEqualTo(1);

    inventory.removeFromCart(Product.BIKE_LIGHT);
    assertThat(inventory.header().cartCount()).isZero();
  }
}
