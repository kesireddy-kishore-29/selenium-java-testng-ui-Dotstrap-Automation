package com.kishore.qa.selenium.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * The validated, immutable configuration of a run. Loaded once (see {@link ConfigLoader}); every
 * value can be overridden by an environment variable ({@code sauce.baseUrl -> SAUCE_BASE_URL}).
 *
 * @param sauceUsername public demo credentials published by Sauce Demo itself
 * @param appBaseUrl the UI flagship's self-hosted app; tests that need it skip when it is down
 * @param gridUrl a Selenium Grid to run on (video mode {@code grid}); empty means a local Chrome
 */
public record Config(
    String environment,
    URI sauceBaseUrl,
    String sauceUsername,
    String saucePassword,
    URI appBaseUrl,
    URI liveBaseUrl,
    boolean headless,
    int windowWidth,
    int windowHeight,
    Duration waitTimeout,
    Duration pageLoadTimeout,
    RecordMode recordMode,
    VideoMode videoMode,
    Optional<URI> gridUrl,
    Path gridVideoDir,
    String ffmpeg,
    int videoFrameRate,
    Path evidenceRoot,
    String runId) {

  /** Lazily loaded on first use, once per JVM; immutable afterwards. */
  private static final class Holder {
    private static final Config INSTANCE = ConfigLoader.load();
  }

  public static Config get() {
    return Holder.INSTANCE;
  }
}
