package com.qaautomation.selenium.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stitches numbered frames ({@code frame-0001.png}, …) into a WebM video with ffmpeg. Frames of
 * different sizes (a test that resizes the window) are scaled and padded into one frame size.
 */
public final class VideoStitcher {

  private static final Logger LOG = LoggerFactory.getLogger(VideoStitcher.class);
  private static final long TIMEOUT_SECONDS = 120;

  private VideoStitcher() {}

  /** The ffmpeg command line; separate so it can be checked without running ffmpeg. */
  static List<String> command(
      String ffmpeg, Path framesDir, int frameRate, int width, int height, Path out) {
    int w = width - width % 2;
    int h = height - height % 2;
    String fit =
        String.format(
            Locale.ROOT,
            "scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2:white",
            w,
            h,
            w,
            h);
    return List.of(
        ffmpeg,
        "-y",
        "-loglevel",
        "error",
        "-framerate",
        String.valueOf(frameRate),
        "-i",
        framesDir.resolve("frame-%04d.png").toString(),
        "-vf",
        fit,
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
        "-pix_fmt",
        "yuv420p",
        out.toString());
  }

  /** Whether the ffmpeg command answers {@code -version}. */
  public static boolean available(String ffmpeg) {
    return run(List.of(ffmpeg, "-version"), 10);
  }

  /** Writes {@code out}; returns false (and logs why) when ffmpeg is missing or fails. */
  public static boolean stitch(
      String ffmpeg, Path framesDir, int frameRate, int width, int height, Path out) {
    return run(command(ffmpeg, framesDir, frameRate, width, height, out), TIMEOUT_SECONDS);
  }

  private static boolean run(List<String> command, long timeoutSeconds) {
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        LOG.warn("{} did not finish within {} s", command.getFirst(), timeoutSeconds);
        return false;
      }
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (process.exitValue() != 0) {
        LOG.warn("{} failed ({}): {}", command.getFirst(), process.exitValue(), output.strip());
        return false;
      }
      return true;
    } catch (IOException e) {
      LOG.warn("{} could not run: {}", command.getFirst(), e.getMessage());
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
