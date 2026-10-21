package com.kishore.qa.selenium.pages.sauce;

import com.kishore.qa.selenium.pages.Accessible;
import com.kishore.qa.selenium.pages.BasePage;
import com.kishore.qa.selenium.pages.Locators;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Sauce Demo sign-in ({@code /}). */
public final class LoginPage extends BasePage {

  private static final By USERNAME = Locators.testId("username");
  private static final By PASSWORD = Locators.testId("password");
  private static final By SUBMIT = Locators.testId("login-button");
  private static final By ERROR = Locators.testId("error");

  public LoginPage(WebDriver driver) {
    super(driver);
  }

  public LoginPage open() {
    driver.get(url(config.sauceBaseUrl(), "/"));
    return awaitShown();
  }

  /** Opens a signed-in page while signed out; Sauce Demo answers with the sign-in form. */
  public LoginPage openProtected(String path) {
    driver.get(url(config.sauceBaseUrl(), path));
    awaitShown();
    wait.visible(ERROR);
    return this;
  }

  LoginPage awaitShown() {
    wait.visible(USERNAME);
    wait.visible(SUBMIT);
    return this;
  }

  /** Signs in and expects to land on the inventory. */
  public InventoryPage signInAs(String username, String password) {
    submit(username, password);
    wait.urlContains("/inventory.html");
    return new InventoryPage(driver).awaitLoaded();
  }

  /** Signs in and expects to stay here with an error. */
  public LoginPage signInExpectingError(String username, String password) {
    submit(username, password);
    wait.visible(ERROR);
    return this;
  }

  public String error() {
    return text(ERROR);
  }

  /** The submit control as a screen reader announces it. */
  public Accessible submitControl() {
    return Accessible.of(wait.visible(SUBMIT));
  }

  private void submit(String username, String password) {
    type(USERNAME, username);
    type(PASSWORD, password);
    click(SUBMIT);
  }
}
