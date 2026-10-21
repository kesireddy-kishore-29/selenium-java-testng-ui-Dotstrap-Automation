package com.kishore.qa.selenium.pages.sauce;

import com.kishore.qa.selenium.config.Config;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

/**
 * A signed-in Sauce Demo session without the sign-in form. Sauce Demo keeps its session in one
 * cookie, so a test that is not about signing in sets that cookie and opens the inventory: one page
 * load instead of a form per test. Only the sign-in tests use the form.
 */
public final class SauceSession {

  private static final String COOKIE = "session-username";

  private SauceSession() {}

  public static InventoryPage signedIn(WebDriver driver) {
    Config config = Config.get();
    // A cookie can only be set for the site the browser is on, so visit it first.
    new LoginPage(driver).open();
    driver.manage().addCookie(new Cookie.Builder(COOKIE, config.sauceUsername()).path("/").build());
    return new InventoryPage(driver).open();
  }
}
