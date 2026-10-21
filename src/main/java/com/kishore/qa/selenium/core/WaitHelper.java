package com.kishore.qa.selenium.core;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Explicit waits only. The driver's implicit wait stays at zero (Checkstyle forbids changing it),
 * so every wait in a test is one of these, with one timeout from the configuration.
 */
public final class WaitHelper {

  private final WebDriver driver;
  private final Duration timeout;

  public WaitHelper(WebDriver driver, Duration timeout) {
    this.driver = driver;
    this.timeout = timeout;
  }

  public WebElement visible(By by) {
    return wait("visibility of " + by).until(ExpectedConditions.visibilityOfElementLocated(by));
  }

  public WebElement clickable(By by) {
    return wait("clickability of " + by).until(ExpectedConditions.elementToBeClickable(by));
  }

  public List<WebElement> allVisible(By by) {
    return wait("visibility of all " + by)
        .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(by));
  }

  public void gone(By by) {
    wait("absence of " + by).until(ExpectedConditions.invisibilityOfElementLocated(by));
  }

  public void urlContains(String fragment) {
    wait("a URL containing '" + fragment + "'").until(ExpectedConditions.urlContains(fragment));
  }

  public void text(By by, String expected) {
    wait("'" + expected + "' in " + by).until(ExpectedConditions.textToBe(by, expected));
  }

  /** Any other condition; the description is what the timeout message says was awaited. */
  public <T> T until(String description, Function<WebDriver, T> condition) {
    return wait(description).until(condition);
  }

  private WebDriverWait wait(String description) {
    WebDriverWait wait = new WebDriverWait(driver, timeout);
    wait.withMessage(() -> "waiting " + timeout.toSeconds() + " s for " + description);
    // A re-render between finding an element and reading it is retried, not a failure.
    wait.ignoring(StaleElementReferenceException.class);
    return wait;
  }
}
