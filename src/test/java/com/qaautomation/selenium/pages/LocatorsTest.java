package com.qaautomation.selenium.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qaautomation.selenium.data.Customer;
import com.qaautomation.selenium.data.Money;
import com.qaautomation.selenium.data.Unique;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

/** Locator construction and the small data helpers page objects rely on. */
public final class LocatorsTest {

  @Test
  public void testIdsAreQuotedAttributeSelectors() {
    assertThat(Locators.testId("add-to-cart-test.allthethings()-t-shirt-(red)"))
        .isEqualTo(By.cssSelector("[data-test=\"add-to-cart-test.allthethings()-t-shirt-(red)\"]"));
    assertThat(Locators.ariaLabel("Delete \"Q1\" report"))
        .isEqualTo(By.cssSelector("[aria-label=\"Delete \\\"Q1\\\" report\"]"));
  }

  @Test
  public void metadataSelectorsAreOnlyForMetaAndLink() {
    assertThat(Locators.metadata("link", "rel", "canonical"))
        .isEqualTo(By.cssSelector("link[rel=\"canonical\"]"));
    assertThatThrownBy(() -> Locators.metadata("button", "type", "submit"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void readsAmountsOutOfDisplayText() {
    assertThat(Money.parse("$29.99")).isEqualByComparingTo("29.99");
    assertThat(Money.parse("Item total: $83.97")).isEqualByComparingTo(new BigDecimal("83.97"));
    assertThat(Money.parse("Tax: $6.72")).isEqualByComparingTo("6.72");
    assertThatThrownBy(() -> Money.parse("Free")).hasMessage("No amount in \"Free\"");
  }

  @Test
  public void incompleteCustomersAreNamedByTheMissingField() {
    Customer customer = Customer.aCustomer().without("postal code");

    assertThat(customer.postalCode()).isNull();
    assertThat(customer.firstName()).isEqualTo("Ada");
    assertThat(customer).hasToString("missing postal code");
  }

  @Test
  public void uniqueNamesCarryTheSweeperPrefix() {
    assertThat(Unique.name("item")).matches("qa-item-[0-9a-f]{8}");
    assertThat(Unique.email()).matches("qa-user-[0-9a-f]{8}@example\\.test");
    assertThat(Unique.name("item")).isNotEqualTo(Unique.name("item"));
  }
}
