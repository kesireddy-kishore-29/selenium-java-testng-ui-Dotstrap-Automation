package com.kishore.qa.selenium.listeners;

import com.kishore.qa.selenium.annotations.UiTest;
import com.kishore.qa.selenium.config.Config;
import com.kishore.qa.selenium.config.RecordMode;
import com.kishore.qa.selenium.config.VideoMode;
import com.kishore.qa.selenium.core.DriverFactory;
import com.kishore.qa.selenium.core.FrameRecorder;
import com.kishore.qa.selenium.core.Redactor;
import com.kishore.qa.selenium.evidence.EvidenceCapture;
import com.kishore.qa.selenium.evidence.FailureBundles;
import com.kishore.qa.selenium.evidence.Gallery;
import com.kishore.qa.selenium.evidence.RunInfo;
import com.kishore.qa.selenium.evidence.RunRecorder;
import com.kishore.qa.selenium.evidence.StepSummary;
import com.kishore.qa.selenium.evidence.TestContext;
import com.kishore.qa.selenium.evidence.TestIdentity;
import com.kishore.qa.selenium.evidence.TestRecord;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Gives every {@link UiTest} method its own browser and turns each attempt into evidence: before
 * the method a browser starts (with a frame recorder when the run keeps evidence); after it,
 * screenshot, full page, page source, console and network log are captured from the still-open
 * browser, the browser quits, and the frames become a video. Failed tests also get a failure
 * bundle; the run gets run-summary.json, flaky-report.json, a gallery and a step summary.
 */
public final class EvidenceListener
    implements ISuiteListener, ITestListener, IInvokedMethodListener {

  private static final Logger LOG = LoggerFactory.getLogger(EvidenceListener.class);
  // "By.cssSelector: [data-test="x"]" up to the end of its line, or up to the "(tried for ...)"
  // that a wait timeout appends. A locator may itself contain parentheses.
  private static final Pattern LOCATOR =
      Pattern.compile("(?m)(By\\.[A-Za-z]+: .+?)(?= \\(tried for | \\(Session info|$)");
  // Chrome's {"method":"css selector","selector":"[data-test="x"]"}: the inner quotes are not
  // escaped, so the selector runs to the closing "} on its line.
  private static final Pattern SELECTOR =
      Pattern.compile("(?m)\"method\":\"([^\"]+)\",\"selector\":\"(.*)\"\\}");
  // Selenium appends build, system, driver and capability details (including the local profile
  // path) to every error. They say nothing about the failure, so reports leave them out.
  private static final Pattern DRIVER_DETAILS =
      Pattern.compile("(?s)\\R?Build info: .*?(?:Session ID: [0-9a-f]+|(?=\\R\\s+at )|\\z)");

  private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
  private final Map<String, List<Path>> attemptDirs = new ConcurrentHashMap<>();
  private final Map<String, Optional<FrameRecorder>> frames = new ConcurrentHashMap<>();
  private final Map<String, Optional<SessionId>> sessions = new ConcurrentHashMap<>();
  private Config config;
  private RunInfo run;
  private RunRecorder recorder;

  @Override
  public synchronized void onStart(ISuite suite) {
    if (recorder == null) {
      config = Config.get();
      run = RunInfo.of(config);
      recorder = new RunRecorder(run);
      LOG.info(
          "Run {}: evidence in {} (RECORD_MODE={}, video {})",
          run.runId(),
          run.runDir(),
          run.recordMode(),
          config.videoMode().name().toLowerCase(Locale.ROOT));
    }
  }

  @Override
  public void beforeInvocation(IInvokedMethod method, ITestResult result) {
    UiTest ui = uiTest(method, result);
    if (ui == null) {
      return;
    }
    TestIdentity identity = TestIdentity.of(result);
    int attempt = attempts.merge(identity.testId(), 1, Integer::sum);
    Path dir =
        run.runDir()
            .resolve(identity.suite())
            .resolve(identity.slug())
            .resolve(String.valueOf(attempt));
    TestContext context = TestContext.begin(identity, attempt, dir);
    Optional<Path> framesDir =
        config.recordMode().recordsWhileRunning() && config.videoMode() == VideoMode.STITCH
            ? Optional.of(dir.resolve(".frames"))
            : Optional.empty();
    DriverFactory.Session session =
        DriverFactory.start(config, framesDir, identity.slug(), ui.live());
    frames.put(key(context), session.frames());
    sessions.put(key(context), Optional.ofNullable(session.raw().getSessionId()));
  }

  /** Runs before Allure closes the test case, so the attachments land inside it. */
  @Override
  public void afterInvocation(IInvokedMethod method, ITestResult result) {
    if (uiTest(method, result) == null) {
      return;
    }
    TestContext context = TestContext.current().orElseThrow();
    boolean failed = !result.isSuccess();
    boolean keep = config.recordMode().keeps(failed);
    context.kept(keep);
    Optional<FrameRecorder> recorderOfFrames = frames.remove(key(context));
    Optional<SessionId> sessionId = sessions.remove(key(context));
    try {
      if (keep) {
        DriverFactory.session().ifPresent(s -> EvidenceCapture.fromBrowser(s.raw(), context));
      }
    } finally {
      DriverFactory.quit();
    }
    if (keep) {
      EvidenceCapture.video(config, recorderOfFrames, sessionId, context);
      attachToAllure(context);
    }
    EvidenceCapture.discardFrames(recorderOfFrames);
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    finish(result, "passed");
  }

  @Override
  public void onTestFailure(ITestResult result) {
    finish(result, "failed");
  }

  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    finish(result, "failed");
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    finish(result, result.wasRetried() ? "retried" : "skipped");
  }

  private void finish(ITestResult result, String status) {
    Optional<TestContext> current = TestContext.current();
    if (current.isEmpty()) {
      recordNotStarted(result, status);
      return;
    }
    TestContext context = current.get();
    try {
      TestIdentity id = context.identity();
      Instant finishedAt = Instant.now();
      Throwable error = result.getThrowable();
      TestRecord record =
          new TestRecord(
              id.testId(),
              id.title(),
              id.suite(),
              id.featureIds(),
              id.groups(),
              status,
              Duration.between(context.startedAt(), finishedAt).toMillis(),
              context.attempt(),
              id.groups().contains("quarantine"),
              context.startedAt(),
              finishedAt,
              error == null ? null : errorInfo(error),
              null);
      if (context.kept()) {
        EvidenceCapture.meta(config, run, context, record);
        List<String> files = new ArrayList<>(context.files());
        files.sort(String::compareTo);
        record =
            new TestRecord(
                record.id(),
                record.title(),
                record.suite(),
                record.featureIds(),
                record.tags(),
                record.status(),
                record.durationMs(),
                record.attempt(),
                record.quarantined(),
                record.startedAt(),
                record.finishedAt(),
                record.error(),
                new TestRecord.EvidenceInfo(
                    run.runDir().relativize(context.attemptDir()).toString().replace('\\', '/'),
                    files));
        EvidenceCapture.meta(config, run, context, record);
        attemptDirs.computeIfAbsent(id.testId(), k -> new ArrayList<>()).add(context.attemptDir());
      }
      recorder.add(record);
    } finally {
      TestContext.end();
    }
  }

  /**
   * A browser test that never started (a failed or skipping configuration method) still belongs in
   * the run summary, as skipped with the reason, or it would silently vanish from the numbers.
   */
  private void recordNotStarted(ITestResult result, String status) {
    if (!result.getMethod().isTest()
        || !result.getTestClass().getRealClass().isAnnotationPresent(UiTest.class)) {
      return;
    }
    TestIdentity id = TestIdentity.of(result);
    Instant at = Instant.now();
    Throwable error = result.getThrowable();
    recorder.add(
        new TestRecord(
            id.testId(),
            id.title(),
            id.suite(),
            id.featureIds(),
            id.groups(),
            status,
            0,
            attempts.merge(id.testId(), 1, Integer::sum),
            id.groups().contains("quarantine"),
            at,
            at,
            error == null ? null : errorInfo(error),
            null));
  }

  @Override
  public synchronized void onFinish(ISuite suite) {
    if (recorder == null || recorder.isEmpty()) {
      return;
    }
    Map<String, Object> environment = new LinkedHashMap<>();
    environment.put("testEnv", config.environment());
    environment.put("sauceBaseUrl", config.sauceBaseUrl().toString());
    environment.put("appBaseUrl", config.appBaseUrl().toString());
    environment.put("browser", "chrome");
    environment.put("headless", config.headless());
    environment.put("videoMode", config.videoMode().name().toLowerCase(Locale.ROOT));
    environment.put("java", System.getProperty("java.version"));
    environment.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
    environment.put("recordMode", run.recordMode());
    RunRecorder.Written written = recorder.write(environment);
    List<TestRecord> finals = recorder.finalResults();
    if (config.recordMode() != RecordMode.OFF) {
      for (TestRecord test : finals) {
        if ("failed".equals(test.status()) && test.evidence() != null) {
          FailureBundles.build(run, test, attemptDirs.getOrDefault(test.id(), List.of()));
        }
      }
    }
    Gallery.write(run, finals);
    StepSummary.write(run, written, finals);
    LOG.info("Run summary: {}", run.runDir().resolve("run-summary.json"));
  }

  private static UiTest uiTest(IInvokedMethod method, ITestResult result) {
    if (!method.isTestMethod()) {
      return null;
    }
    return result.getTestClass().getRealClass().getAnnotation(UiTest.class);
  }

  private static String key(TestContext context) {
    return context.identity().testId() + "#" + context.attempt();
  }

  private static void attachToAllure(TestContext context) {
    for (String file : List.of("screenshot.png", "video.webm", "page-source.html", "console.log")) {
      Path path = context.attemptDir().resolve(file);
      if (!Files.exists(path)) {
        continue;
      }
      String type =
          switch (file) {
            case "screenshot.png" -> "image/png";
            case "video.webm" -> "video/webm";
            case "page-source.html" -> "text/html";
            default -> "text/plain";
          };
      try {
        Allure.addAttachment(
            file,
            type,
            new ByteArrayInputStream(Files.readAllBytes(path)),
            file.substring(file.lastIndexOf('.') + 1));
      } catch (IOException e) {
        LOG.debug("Not attached to Allure: {}", file);
      }
    }
  }

  static TestRecord.ErrorInfo errorInfo(Throwable error) {
    StringWriter stack = new StringWriter();
    error.printStackTrace(new PrintWriter(stack));
    String text = Redactor.redact(withoutDriverDetails(stack.toString()));
    String message = Redactor.redact(withoutDriverDetails(String.valueOf(error.getMessage())));
    return new TestRecord.ErrorInfo(
        error.getClass().getName(),
        message,
        text.length() > 6000 ? text.substring(0, 6000) : text,
        failingLocator(message));
  }

  static String withoutDriverDetails(String text) {
    return DRIVER_DETAILS.matcher(text).replaceAll("").strip();
  }

  /** The locator Selenium could not find, from a wait timeout or a NoSuchElementException. */
  static String failingLocator(String message) {
    Matcher by = LOCATOR.matcher(message);
    if (by.find()) {
      return by.group(1).strip();
    }
    Matcher selector = SELECTOR.matcher(message);
    if (selector.find()) {
      return "By." + toCamel(selector.group(1)) + ": " + selector.group(2);
    }
    return null;
  }

  private static String toCamel(String method) {
    String[] words = method.split(" ");
    StringBuilder out = new StringBuilder(words[0]);
    for (int i = 1; i < words.length; i++) {
      out.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
    }
    return out.toString();
  }
}
