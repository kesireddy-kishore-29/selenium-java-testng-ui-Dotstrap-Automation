package com.kishore.qa.selenium.pages.live;

import com.kishore.qa.selenium.core.BrowserLogs;
import com.kishore.qa.selenium.core.DriverFactory;
import com.kishore.qa.selenium.evidence.TestContext;
import com.kishore.qa.selenium.pages.Accessible;
import com.kishore.qa.selenium.pages.BasePage;
import com.kishore.qa.selenium.pages.Locators;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;

/**
 * Any public page of the live site, read the way a visitor's browser reads it: the document status
 * from Chrome's performance log, the rendered first heading (not only the status: a client-rendered
 * page can answer 200 and show nothing), metadata, console errors and layout width. Read-only: it
 * never types, submits or signs in.
 */
public final class PublicPage extends BasePage {

  private static final By HEADING = Locators.tag("h1");

  /** Page wider than its viewport, and up to five elements that push it out. */
  public record Overflow(long documentWidth, long viewportWidth, List<String> offenders) {}

  private final TestContext context;

  public PublicPage(WebDriver driver) {
    super(driver);
    this.context =
        TestContext.current()
            .orElseThrow(() -> new IllegalStateException("PublicPage needs a @UiTest context"));
  }

  public PublicPage open(String path) {
    driver.get(url(config.liveBaseUrl(), path));
    wait.until(
        "document to finish loading",
        d ->
            "complete"
                .equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    context.collectLogs(driver);
    return this;
  }

  /**
   * Sets the viewport (not the window) to exactly {@code width} CSS pixels, as a phone or a small
   * laptop would have it. Resizing the window cannot do this: the window frame takes pixels, and
   * Chrome refuses windows narrower than about 500 px. The live smoke always runs on a local
   * Chrome, which accepts the DevTools command.
   */
  public PublicPage viewport(int width, int height) {
    if (!(DriverFactory.session().orElseThrow().raw() instanceof HasCdp chrome)) {
      throw new IllegalStateException("Setting an exact viewport needs a local Chrome");
    }
    chrome.executeCdpCommand(
        "Emulation.setDeviceMetricsOverride",
        Map.of("width", width, "height", height, "deviceScaleFactor", 1, "mobile", false));
    return this;
  }

  /** The HTTP status of the page's own document, as Chrome's performance log recorded it. */
  public Optional<Integer> documentStatus() {
    context.collectLogs(driver);
    return BrowserLogs.documentStatus(context.network(), driver.getCurrentUrl());
  }

  public String title() {
    return driver.getTitle();
  }

  /** The first level-1 heading, once rendered, as a screen reader announces it. */
  public Accessible heading() {
    WebElement heading = wait.visible(HEADING);
    wait.until("the first heading to have text", d -> !heading.getText().isBlank());
    return Accessible.of(heading);
  }

  public Optional<String> metaDescription() {
    return attribute(Locators.metadata("meta", "name", "description"), "content");
  }

  public Optional<String> canonical() {
    return attribute(Locators.metadata("link", "rel", "canonical"), "href");
  }

  /** Console messages at error level seen so far in this test, on any page. */
  public List<String> consoleErrors() {
    context.collectLogs(driver);
    return context.console().stream().filter(line -> line.startsWith("[SEVERE]")).toList();
  }

  /**
   * Responses of 400 or more and network failures seen so far. A request the browser cancelled
   * itself (net::ERR_ABORTED: a navigation or prefetch) is not a failure the site served.
   */
  public List<String> failedRequests() {
    context.collectLogs(driver);
    List<String> failed = new ArrayList<>();
    for (BrowserLogs.NetworkEntry entry : context.network()) {
      if (entry.status() != null && entry.status() >= 400) {
        failed.add(entry.status() + " " + entry.method() + " " + entry.url());
      } else if (entry.error() != null && !entry.error().contains("ERR_ABORTED")) {
        failed.add(entry.error() + " " + entry.method() + " " + entry.url());
      }
    }
    return failed;
  }

  /** What the browser tells the site it is (the live smoke names this project in it). */
  public String userAgent() {
    return String.valueOf(
        ((JavascriptExecutor) driver).executeScript("return navigator.userAgent"));
  }

  /** The layout viewport width in CSS pixels, scrollbar included. */
  public long innerWidth() {
    Object width = ((JavascriptExecutor) driver).executeScript("return window.innerWidth");
    return ((Number) width).longValue();
  }

  public Overflow horizontalOverflow() {
    Object raw =
        ((JavascriptExecutor) driver)
            .executeScript(
                """
                const viewportWidth = document.documentElement.clientWidth;
                const offenders = [];
                for (const el of document.body.querySelectorAll('*')) {
                  const rect = el.getBoundingClientRect();
                  if (rect.width > 0 && rect.right > viewportWidth + 1) {
                    offenders.push(el.tagName.toLowerCase() + (el.id ? '#' + el.id : '')
                      + ' (right edge ' + Math.round(rect.right) + 'px)');
                  }
                }
                return { documentWidth: document.documentElement.scrollWidth,
                         viewportWidth, offenders: offenders.slice(0, 5) };
                """);
    if (!(raw instanceof Map<?, ?> result)) {
      throw new IllegalStateException("Unexpected overflow result: " + raw);
    }
    List<String> offenders = new ArrayList<>();
    if (result.get("offenders") instanceof List<?> list) {
      list.forEach(o -> offenders.add(String.valueOf(o)));
    }
    return new Overflow(
        ((Number) result.get("documentWidth")).longValue(),
        ((Number) result.get("viewportWidth")).longValue(),
        offenders);
  }

  private Optional<String> attribute(By by, String attribute) {
    return driver.findElements(by).stream()
        .findFirst()
        .map(e -> e.getDomAttribute(attribute))
        .filter(v -> !v.isBlank());
  }
}
