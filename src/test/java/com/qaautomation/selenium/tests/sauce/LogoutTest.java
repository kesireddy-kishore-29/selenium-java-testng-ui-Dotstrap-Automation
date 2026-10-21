package com.qaautomation.selenium.tests.sauce;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaautomation.selenium.annotations.Feature;
import com.qaautomation.selenium.annotations.UiTest;
import com.qaautomation.selenium.core.DriverFactory;
import com.qaautomation.selenium.pages.Accessible;
import com.qaautomation.selenium.pages.sauce.InventoryPage;
import com.qaautomation.selenium.pages.sauce.LoginPage;
import com.qaautomation.selenium.pages.sauce.SauceSession;
import org.testng.annotations.Test;

/** Sauce Demo logout. */
@UiTest
public final class LogoutTest {

  @Test(
      groups = "regression",
      description = "logging out returns to sign-in and the inventory is no longer reachable")
  @Feature("FEAT-LOGOUT-01")
  public void logoutEndsTheSession() {
    InventoryPage inventory = SauceSession.signedIn(DriverFactory.driver());
    assertThat(inventory.header().menuButton()).isEqualTo(new Accessible("button", "Open Menu"));

    LoginPage login = inventory.logout();
    assertThat(login.currentPath()).isEqualTo("/");

    login.openProtected("/inventory.html");
    assertThat(login.error())
        .contains("You can only access '/inventory.html' when you are logged in.");
  }
}
