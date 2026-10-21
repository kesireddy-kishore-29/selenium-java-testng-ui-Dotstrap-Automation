package com.qaautomation.selenium.evidence;

import com.qaautomation.selenium.config.Config;
import com.qaautomation.selenium.config.VideoMode;
import com.qaautomation.selenium.core.FrameRecorder;
import com.qaautomation.selenium.core.Json;
import com.qaautomation.selenium.core.Redactor;
import com.qaautomation.selenium.core.VideoStitcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes one attempt's evidence into its folder: screenshot, full-page screenshot, page source,
 * console log and network log while the browser is still open, then the video after it has quit.
 * Every text artifact is redacted. A capture that fails is noted in meta.json, never fatal.
 */
public final class EvidenceCapture {

  private static final Logger LOG = LoggerFactory.getLogger(EvidenceCapture.class);

  private EvidenceCapture() {}

  /** Everything that needs the live browser. Call before the browser quits. */
  public static void fromBrowser(RemoteWebDriver raw, TestContext context) {
    Path dir = context.attemptDir();
    mkdirs(dir);
    attempt(context, "screenshot.png", () -> raw.getScreenshotAs(OutputType.BYTES));
    if (raw instanceof ChromeDriver chrome) {
      attempt(context, "fullpage.png", () -> fullPage(chrome));
    }
    attempt(
        context,
        "page-source.html",
        () -> Redactor.redact(raw.getPageSource()).getBytes(StandardCharsets.UTF_8));
    context.collectLogs(raw);
    write(
        context,
        "console.log",
        String.join(
                "\n",
                context.console().isEmpty() ? List.of("(no console output)") : context.console())
            .getBytes(StandardCharsets.UTF_8));
    Json.write(dir.resolve("network-log.json"), context.network());
    context.wrote("network-log.json");
  }

  /** The video, after the browser has quit (the last frame is the failure screenshot). */
  public static void video(
      Config config,
      Optional<FrameRecorder> frames,
      Optional<SessionId> sessionId,
      TestContext context) {
    Path out = context.attemptDir().resolve("video.webm");
    if (config.videoMode() == VideoMode.STITCH && frames.isPresent()) {
      FrameRecorder recorder = frames.get();
      Path screenshot = context.attemptDir().resolve("screenshot.png");
      if (Files.exists(screenshot)) {
        copy(
            screenshot,
            recorder
                .dir()
                .resolve(String.format(Locale.ROOT, "frame-%04d.png", recorder.frames() + 1)));
      }
      // The video takes the size of the first frame (the viewport), not of the window, so the
      // picture is not padded with the space the window frame takes.
      int[] size =
          pngSize(recorder.dir().resolve("frame-0001.png"))
              .orElse(new int[] {config.windowWidth(), config.windowHeight()});
      if (recorder.frames() == 0) {
        context.note("no page action was recorded, so there is no video");
      } else if (VideoStitcher.stitch(
          config.ffmpeg(), recorder.dir(), config.videoFrameRate(), size[0], size[1], out)) {
        context.wrote("video.webm");
      } else {
        keepFramesAsGallery(recorder.dir(), context);
      }
    } else if (config.videoMode() == VideoMode.GRID && sessionId.isPresent()) {
      gridVideo(config, sessionId.get(), out, context);
    }
  }

  /** Removes the raw frames once they are in the video (or no longer needed). */
  public static void discardFrames(Optional<FrameRecorder> frames) {
    frames
        .map(FrameRecorder::dir)
        .filter(Files::isDirectory)
        .ifPresent(EvidenceCapture::deleteTree);
  }

  /** meta.json: identity, environment and notes, for people and for triage. */
  public static void meta(Config config, RunInfo run, TestContext context, TestRecord record) {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("schemaVersion", 1);
    meta.put("repo", run.repo());
    meta.put("runId", run.runId());
    meta.put("gitSha", run.gitSha());
    meta.put("recordMode", run.recordMode());
    meta.put("test", record);
    Map<String, Object> environment = new LinkedHashMap<>();
    environment.put("browser", "chrome");
    environment.put("headless", config.headless());
    environment.put("window", config.windowWidth() + "x" + config.windowHeight());
    environment.put("videoMode", config.videoMode().name().toLowerCase(Locale.ROOT));
    environment.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
    environment.put("java", System.getProperty("java.version"));
    meta.put("environment", environment);
    meta.put("notes", context.notes());
    Json.write(context.attemptDir().resolve("meta.json"), meta);
    context.wrote("meta.json");
  }

  /** Width and height from a PNG's header (IHDR: bytes 16-23), without decoding the image. */
  static Optional<int[]> pngSize(Path png) {
    try (InputStream in = Files.newInputStream(png)) {
      byte[] header = in.readNBytes(24);
      if (header.length < 24 || header[1] != 'P' || header[2] != 'N' || header[3] != 'G') {
        return Optional.empty();
      }
      ByteBuffer ihdr = ByteBuffer.wrap(header, 16, 8);
      return Optional.of(new int[] {ihdr.getInt(), ihdr.getInt()});
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private static byte[] fullPage(ChromeDriver chrome) {
    Map<String, Object> metrics = chrome.executeCdpCommand("Page.getLayoutMetrics", Map.of());
    Object size = metrics.get("cssContentSize");
    if (!(size instanceof Map<?, ?> content)) {
      throw new IllegalStateException("no content size from Page.getLayoutMetrics");
    }
    double width = ((Number) content.get("width")).doubleValue();
    double height = Math.min(((Number) content.get("height")).doubleValue(), 16_000);
    Map<String, Object> clip = Map.of("x", 0, "y", 0, "width", width, "height", height, "scale", 1);
    Map<String, Object> shot =
        chrome.executeCdpCommand(
            "Page.captureScreenshot",
            Map.of("format", "png", "captureBeyondViewport", true, "clip", clip));
    return Base64.getDecoder().decode(String.valueOf(shot.get("data")));
  }

  private static void gridVideo(Config config, SessionId sessionId, Path out, TestContext context) {
    Path dir = config.gridVideoDir();
    Optional<Path> video = findGridVideo(dir, sessionId.toString());
    if (video.isEmpty()) {
      context.note("grid video for session " + sessionId + " not found in " + dir);
      return;
    }
    if (VideoStitcher.available(config.ffmpeg())) {
      if (transcode(config.ffmpeg(), video.get(), out)) {
        context.wrote("video.webm");
        return;
      }
    }
    copy(video.get(), context.attemptDir().resolve("video.mp4"));
    context.wrote("video.mp4");
  }

  static Optional<Path> findGridVideo(Path dir, String sessionId) {
    if (!Files.isDirectory(dir)) {
      return Optional.empty();
    }
    try (Stream<Path> files = Files.walk(dir, 2)) {
      return files
          .filter(Files::isRegularFile)
          .filter(f -> f.getFileName().toString().contains(sessionId))
          .max(Comparator.comparingLong(f -> f.toFile().lastModified()));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private static boolean transcode(String ffmpeg, Path in, Path out) {
    try {
      Process p =
          new ProcessBuilder(
                  ffmpeg,
                  "-y",
                  "-loglevel",
                  "error",
                  "-i",
                  in.toString(),
                  "-c:v",
                  "libvpx-vp9",
                  "-b:v",
                  "0",
                  "-crf",
                  "40",
                  "-deadline",
                  "realtime",
                  "-cpu-used",
                  "8",
                  out.toString())
              .redirectErrorStream(true)
              .start();
      return p.waitFor(120, TimeUnit.SECONDS) && p.exitValue() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static void keepFramesAsGallery(Path frames, TestContext context) {
    Path target = context.attemptDir().resolve("frames");
    try (Stream<Path> files = Files.list(frames)) {
      Files.createDirectories(target);
      for (Path frame : files.sorted().toList()) {
        Files.copy(frame, target.resolve(frame.getFileName()), StandardCopyOption.REPLACE_EXISTING);
      }
      context.wrote("frames");
      context.note("ffmpeg was not available: the frames are kept as numbered screenshots");
    } catch (IOException e) {
      context.note("frames could not be kept: " + e.getMessage());
    }
  }

  @FunctionalInterface
  private interface Capture {
    byte[] take() throws IOException;
  }

  private static void attempt(TestContext context, String file, Capture capture) {
    try {
      write(context, file, capture.take());
    } catch (WebDriverException | IOException | IllegalStateException e) {
      LOG.debug("{} not captured: {}", file, e.getMessage());
      context.note(file + " not captured: " + firstLine(e.getMessage()));
    }
  }

  private static void write(TestContext context, String file, byte[] content) {
    try {
      Files.write(context.attemptDir().resolve(file), content);
      context.wrote(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void copy(Path from, Path to) {
    try {
      Files.createDirectories(to.getParent());
      Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void mkdirs(Path dir) {
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static void deleteTree(Path dir) {
    try (Stream<Path> files = Files.walk(dir)) {
      for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(p);
      }
    } catch (IOException e) {
      LOG.debug("Could not delete {}: {}", dir, e.getMessage());
    }
  }

  private static String firstLine(String text) {
    return text == null ? "" : text.strip().lines().findFirst().orElse("");
  }
}
