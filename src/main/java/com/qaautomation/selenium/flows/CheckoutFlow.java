package com.qaautomation.selenium.flows;

import com.qaautomation.selenium.data.Customer;
import com.qaautomation.selenium.data.Product;
import com.qaautomation.selenium.pages.sauce.CheckoutInformationPage;
import com.qaautomation.selenium.pages.sauce.CheckoutOverviewPage;
import com.qaautomation.selenium.pages.sauce.InventoryPage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The steps every checkout test repeats, as one fluent sentence:
 *
 * <pre>{@code
 * CheckoutFlow.from(inventory).add(BACKPACK, ONESIE).as(aCustomer()).finish();
 * }</pre>
 *
 * A flow only chains page-object calls; it holds no locators and makes no assertions.
 */
public final class CheckoutFlow {

  private final InventoryPage inventory;
  private final List<Product> products = new ArrayList<>();

  private CheckoutFlow(InventoryPage inventory) {
    this.inventory = inventory;
  }

  public static CheckoutFlow from(InventoryPage inventory) {
    return new CheckoutFlow(inventory);
  }

  public CheckoutFlow add(Product... more) {
    products.addAll(Arrays.asList(more));
    return this;
  }

  /** Adds the products, opens the cart and starts checkout. Ends on step one. */
  public CheckoutInformationPage startCheckout() {
    for (Product product : products) {
      inventory.addToCart(product);
    }
    return inventory.openCart().checkout();
  }

  /** Starts checkout and enters the customer. Ends on the overview. */
  public CheckoutOverviewPage as(Customer customer) {
    return startCheckout().fill(customer).continueToOverview();
  }
}
