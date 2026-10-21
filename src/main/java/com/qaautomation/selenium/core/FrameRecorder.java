package com.qaautomation.selenium.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selenium has no video recorder, so this listener takes a screenshot after every page action
 * (navigation, click, typing, resize) into numbered frames. {@link VideoStitcher} turns them into
 * {@code video.webm}. Registered only when the run keeps evidence and the video mode is {@code
 * stitch}; one recorder per test, never shared between threads.
 */
public final class FrameRecorder implements WebDriverListener {

  private static final Logger LOG = LoggerFactory.getLogger(FrameRecorder.class);

  private final Path dir;
  private final TakesScreenshot camera;
  private final AtomicInteger count = new AtomicInteger();

  /**
   * @param camera the undecorated driver, so taking a frame never fires another event
   */
  public FrameRecorder(Path dir, TakesScreenshot camera) {
    this.dir = dir;
    this.camera = camera;
  }

  public Path dir() {
    return dir;
  }

  public int frames() {
    return count.get();
  }

  /** One frame now. A failed capture (page closing, alert open) is logged and skipped. */
  public void capture() {
    try {
      byte[] png = camera.getScreenshotAs(OutputType.BYTES);
      Files.createDirectories(dir);
      String name = String.format(Locale.ROOT, "frame-%04d.png", count.incrementAndGet());
      Files.write(dir.resolve(name), png);
    } catch (WebDriverException | IOException e) {
      LOG.debug("Frame not captured: {}", e.getMessage());
    }
  }

  @Override
  public void afterGet(WebDriver driver, String url) {
    capture();
  }

  @Override
  public void afterClick(WebElement element) {
    capture();
  }

  @Override
  public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
    capture();
  }

  @Override
  public void afterClear(WebElement element) {
    capture();
  }

  @Override
  public void afterTo(WebDriver.Navigation navigation, String url) {
    capture();
  }

  @Override
  public void afterBack(WebDriver.Navigation navigation) {
    capture();
  }

  @Override
  public void afterRefresh(WebDriver.Navigation navigation) {
    capture();
  }

  @Override
  public void afterSetSize(WebDriver.Window window, Dimension size) {
    capture();
  }
}
