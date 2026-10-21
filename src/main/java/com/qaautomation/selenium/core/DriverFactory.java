package com.qaautomation.selenium.core;

import com.qaautomation.selenium.config.Config;
import com.qaautomation.selenium.config.VideoMode;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.events.EventFiringDecorator;

/**
 * One browser per test thread. TestNG runs test methods in parallel, so the driver lives in a
 * {@link ThreadLocal}: a page object always talks to the browser of the test that created it.
 * Drivers are started and quit by the evidence listener, never by tests.
 */
public final class DriverFactory {

  /** The user agent suffix the live smoke presents, so the site owner can see who is visiting. */
  public static final String LIVE_USER_AGENT_SUFFIX =
      " selenium-java-ui-framework (portfolio smoke test)";

  /** A started browser: the driver tests use, the raw one evidence uses, and its recorder. */
  public record Session(WebDriver driver, RemoteWebDriver raw, Optional<FrameRecorder> frames) {}

  private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

  private DriverFactory() {}

  /**
   * Starts a browser for the current thread.
   *
   * @param framesDir where to record frames for the stitched video, or empty for no frames
   * @param testName the test's slug; names the session on a Grid (and its video)
   * @param liveVisitor true for the live smoke: always a local Chrome, with a user agent naming us
   */
  public static Session start(
      Config config, Optional<Path> framesDir, String testName, boolean liveVisitor) {
    if (CURRENT.get() != null) {
      throw new IllegalStateException("A browser is already running on this thread");
    }
    ChromeOptions options = options(config);
    boolean grid = config.videoMode() == VideoMode.GRID && !liveVisitor;
    RemoteWebDriver raw;
    if (grid) {
      options.setCapability("se:recordVideo", true);
      options.setCapability("se:name", testName);
      try {
        raw = new RemoteWebDriver(config.gridUrl().orElseThrow().toURL(), options);
      } catch (MalformedURLException e) {
        throw new IllegalStateException("grid.url is not a URL", e);
      }
    } else {
      raw = new ChromeDriver(options);
    }
    raw.manage().timeouts().pageLoadTimeout(config.pageLoadTimeout());
    raw.manage().window().setSize(new Dimension(config.windowWidth(), config.windowHeight()));
    if (liveVisitor && raw instanceof ChromeDriver chrome) {
      String agent = String.valueOf(chrome.executeScript("return navigator.userAgent"));
      chrome.executeCdpCommand(
          "Emulation.setUserAgentOverride", Map.of("userAgent", agent + LIVE_USER_AGENT_SUFFIX));
    }
    Optional<FrameRecorder> frames = framesDir.map(dir -> new FrameRecorder(dir, raw));
    WebDriver driver =
        frames.isPresent()
            ? new EventFiringDecorator<>(WebDriver.class, frames.get()).decorate(raw)
            : raw;
    Session session = new Session(driver, raw, frames);
    CURRENT.set(session);
    return session;
  }

  /** The browser of the test running on this thread. */
  public static WebDriver driver() {
    return current().driver();
  }

  public static Optional<Session> session() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static Optional<SessionId> sessionId() {
    return session().map(s -> s.raw().getSessionId());
  }

  /** Quits this thread's browser, if any. Safe to call twice. */
  public static void quit() {
    Session session = CURRENT.get();
    CURRENT.remove();
    if (session != null) {
      session.raw().quit();
    }
  }

  private static Session current() {
    Session session = CURRENT.get();
    if (session == null) {
      throw new IllegalStateException(
          "No browser on this thread. Is the test class annotated @UiTest?");
    }
    return session;
  }

  private static ChromeOptions options(Config config) {
    ChromeOptions options = new ChromeOptions();
    if (config.headless()) {
      options.addArguments("--headless=new");
    }
    options.addArguments("--window-size=" + config.windowWidth() + "," + config.windowHeight());
    // Console and network logs for the evidence; read through BrowserLogs.
    options.setCapability("goog:loggingPrefs", Map.of("browser", "ALL", "performance", "ALL"));
    return options;
  }
}
