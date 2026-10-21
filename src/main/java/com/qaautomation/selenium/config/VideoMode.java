package com.qaautomation.selenium.config;

import java.util.Locale;

/**
 * How a test's video is made (Selenium has no built-in recorder). See docs/adr for the trade-off.
 */
public enum VideoMode {
  /** A screenshot after every page action, stitched into video.webm with ffmpeg. Works anywhere. */
  STITCH,
  /** A Selenium Grid in Docker records the session; the file is mapped to the test afterwards. */
  GRID,
  /** No video. */
  OFF;

  public static VideoMode parse(String raw) {
    try {
      return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("'" + raw + "' is not one of stitch, grid, off", e);
    }
  }
}
