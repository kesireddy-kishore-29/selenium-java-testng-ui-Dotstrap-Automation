package com.qaautomation.selenium.pages.sauce;

import com.qaautomation.selenium.data.Customer;
import com.qaautomation.selenium.pages.BasePage;
import com.qaautomation.selenium.pages.Locators;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Checkout step one: who is buying ({@code /checkout-step-one.html}). */
public final class CheckoutInformationPage extends BasePage {

  private static final By FIRST_NAME = Locators.testId("firstName");
  private static final By LAST_NAME = Locators.testId("lastName");
  private static final By POSTAL_CODE = Locators.testId("postalCode");
  private static final By CONTINUE = Locators.testId("continue");
  private static final By ERROR = Locators.testId("error");

  public CheckoutInformationPage(WebDriver driver) {
    super(driver);
  }

  CheckoutInformationPage awaitLoaded() {
    wait.urlContains("/checkout-step-one.html");
    wait.visible(FIRST_NAME);
    return this;
  }

  /** Types every field the customer has; a {@code null} field stays empty. */
  public CheckoutInformationPage fill(Customer customer) {
    fillIfPresent(FIRST_NAME, customer.firstName());
    fillIfPresent(LAST_NAME, customer.lastName());
    fillIfPresent(POSTAL_CODE, customer.postalCode());
    return this;
  }

  public CheckoutOverviewPage continueToOverview() {
    click(CONTINUE);
    return new CheckoutOverviewPage(driver).awaitLoaded();
  }

  /** Presses Continue and expects to stay on this step with an error. */
  public CheckoutInformationPage continueExpectingError() {
    click(CONTINUE);
    wait.visible(ERROR);
    return this;
  }

  public String error() {
    return text(ERROR);
  }

  private void fillIfPresent(By field, String value) {
    if (value != null) {
      type(field, value);
    }
  }
}
