package com.kishore.qa.selenium.evidence;

import com.kishore.qa.selenium.core.BrowserLogs;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.WebDriver;

/**
 * What one attempt of one test has seen so far, held per thread: identity, attempt number, where
 * its evidence goes, and the browser logs read so far (reading Chrome's logs empties them, so they
 * accumulate here for the evidence and for checks such as "no console errors").
 */
public final class TestContext {

  private static final ThreadLocal<TestContext> CURRENT = new ThreadLocal<>();

  private final TestIdentity identity;
  private final int attempt;
  private final Path attemptDir;
  private final Instant startedAt = Instant.now();
  private final List<String> console = new ArrayList<>();
  private final List<BrowserLogs.NetworkEntry> network = new ArrayList<>();
  private final List<String> notes = new ArrayList<>();
  private final List<String> files = new ArrayList<>();
  private boolean kept;

  private TestContext(TestIdentity identity, int attempt, Path attemptDir) {
    this.identity = identity;
    this.attempt = attempt;
    this.attemptDir = attemptDir;
  }

  public static TestContext begin(TestIdentity identity, int attempt, Path attemptDir) {
    TestContext context = new TestContext(identity, attempt, attemptDir);
    CURRENT.set(context);
    return context;
  }

  public static Optional<TestContext> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static void end() {
    CURRENT.remove();
  }

  /** Reads the browser's console and network logs into this context. */
  public synchronized void collectLogs(WebDriver driver) {
    console.addAll(BrowserLogs.console(driver));
    network.addAll(BrowserLogs.network(driver));
  }

  public synchronized List<String> console() {
    return List.copyOf(console);
  }

  public synchronized List<BrowserLogs.NetworkEntry> network() {
    return List.copyOf(network);
  }

  public synchronized void note(String note) {
    notes.add(note);
  }

  public synchronized List<String> notes() {
    return List.copyOf(notes);
  }

  public synchronized void wrote(String file) {
    if (!files.contains(file)) {
      files.add(file);
    }
  }

  public synchronized List<String> files() {
    return List.copyOf(files);
  }

  public synchronized void kept(boolean value) {
    kept = value;
  }

  public synchronized boolean kept() {
    return kept;
  }

  public TestIdentity identity() {
    return identity;
  }

  public int attempt() {
    return attempt;
  }

  public Path attemptDir() {
    return attemptDir;
  }

  public Instant startedAt() {
    return startedAt;
  }
}
