package com.qaautomation.selenium.pages;

import com.qaautomation.selenium.config.Config;
import com.qaautomation.selenium.core.WaitHelper;
import java.net.URI;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * What every page object shares: the browser of the current test, the explicit waits and a few
 * actions that always wait first. A page object exposes intentions ({@code signInAs}, {@code
 * addToCart}); tests never see a locator or a {@link WebElement}.
 */
public abstract class BasePage {

  protected final WebDriver driver;
  protected final Config config;
  protected final WaitHelper wait;

  protected BasePage(WebDriver driver) {
    this.driver = driver;
    this.config = Config.get();
    this.wait = new WaitHelper(driver, config.waitTimeout());
  }

  /** The path of the current URL, for "where did we end up" assertions. */
  public String currentPath() {
    return URI.create(driver.getCurrentUrl()).getPath();
  }

  protected void click(By by) {
    wait.clickable(by).click();
  }

  protected void type(By by, String text) {
    WebElement field = wait.visible(by);
    field.clear();
    field.sendKeys(text);
  }

  protected String text(By by) {
    return wait.visible(by).getText().strip();
  }

  protected boolean present(By by) {
    return !driver.findElements(by).isEmpty();
  }

  protected static String url(URI base, String path) {
    String root = base.toString();
    return (root.endsWith("/") ? root.substring(0, root.length() - 1) : root) + path;
  }
}
