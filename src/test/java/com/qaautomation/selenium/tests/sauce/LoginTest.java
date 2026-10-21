package com.qaautomation.selenium.tests.sauce;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaautomation.selenium.annotations.Feature;
import com.qaautomation.selenium.annotations.UiTest;
import com.qaautomation.selenium.config.Config;
import com.qaautomation.selenium.core.DriverFactory;
import com.qaautomation.selenium.pages.Accessible;
import com.qaautomation.selenium.pages.sauce.InventoryPage;
import com.qaautomation.selenium.pages.sauce.LoginPage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Sauce Demo sign-in. The only tests that use the form; every other test seeds the session. */
@UiTest
public final class LoginTest {

  /** One refused sign-in; {@link #toString()} is the name reports show. */
  public record Attempt(String label, String username, String password, String error) {
    @Override
    public String toString() {
      return label;
    }
  }

  @Test(
      groups = {"smoke", "regression"},
      description = "valid credentials reach the inventory")
  @Feature("FEAT-LOGIN-01")
  public void validCredentialsReachTheInventory() {
    Config config = Config.get();
    LoginPage login = new LoginPage(DriverFactory.driver()).open();
    assertThat(login.submitControl()).isEqualTo(new Accessible("button", "Login"));

    InventoryPage inventory = login.signInAs(config.sauceUsername(), config.saucePassword());

    assertThat(inventory.currentPath()).isEqualTo("/inventory.html");
    assertThat(inventory.title()).isEqualTo("Products");
  }

  @DataProvider(name = "invalidCredentials", parallel = true)
  public Object[][] invalidCredentials() {
    String noMatch = "Username and password do not match any user in this service";
    return new Object[][] {
      {new Attempt("wrong password", "standard_user", "not-the-password", noMatch)},
      {new Attempt("unknown user", "nobody_here", "secret_sauce", noMatch)},
      {new Attempt("empty username", "", "secret_sauce", "Username is required")},
      {new Attempt("empty password", "standard_user", "", "Password is required")},
    };
  }

  @Test(
      dataProvider = "invalidCredentials",
      groups = "regression",
      description = "invalid credentials are refused with a specific message")
  @Feature("FEAT-LOGIN-02")
  public void invalidCredentialsAreRefused(Attempt attempt) {
    LoginPage login =
        new LoginPage(DriverFactory.driver())
            .open()
            .signInExpectingError(attempt.username(), attempt.password());

    assertThat(login.error()).contains(attempt.error());
    assertThat(login.currentPath()).isEqualTo("/");
  }

  @Test(
      groups = "regression",
      description = "a locked-out user is refused with the lock-out message")
  @Feature("FEAT-LOGIN-03")
  public void lockedOutUserIsRefused() {
    LoginPage login =
        new LoginPage(DriverFactory.driver())
            .open()
            .signInExpectingError("locked_out_user", Config.get().saucePassword());

    assertThat(login.error()).contains("Sorry, this user has been locked out.");
    assertThat(login.currentPath()).isEqualTo("/");
  }
}
