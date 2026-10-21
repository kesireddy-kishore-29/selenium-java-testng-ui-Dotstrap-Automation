package com.qaautomation.selenium.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.testng.SkipException;
import org.testng.annotations.Test;

public final class VideoStitcherTest {

  @Test
  public void buildsAnFfmpegCommandWithEvenDimensionsAndTheFramePattern() {
    Path frames = Path.of("frames");
    List<String> command =
        VideoStitcher.command("ffmpeg", frames, 2, 1367, 901, Path.of("video.webm"));

    assertThat(command.getFirst()).isEqualTo("ffmpeg");
    assertThat(command).containsSequence("-framerate", "2");
    assertThat(command).containsSequence("-i", frames.resolve("frame-%04d.png").toString());
    assertThat(command).containsSequence("-c:v", "libvpx-vp9");
    assertThat(command.get(command.indexOf("-vf") + 1))
        .isEqualTo(
            "scale=1366:900:force_original_aspect_ratio=decrease,"
                + "pad=1366:900:(ow-iw)/2:(oh-ih)/2:white");
    assertThat(command.getLast()).isEqualTo("video.webm");
  }

  @Test
  public void stitchesRealFramesIntoAWebmVideo() throws IOException {
    String ffmpeg = System.getProperty("video.ffmpeg", "ffmpeg");
    if (!VideoStitcher.available(ffmpeg)) {
      throw new SkipException("ffmpeg is not on the PATH");
    }
    Path dir = Files.createTempDirectory("frames");
    for (int i = 1; i <= 3; i++) {
      BufferedImage frame = new BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = frame.createGraphics();
      g.setColor(new Color(40 * i, 80, 160));
      g.fillRect(0, 0, 320, 200);
      g.dispose();
      ImageIO.write(
          frame, "png", dir.resolve(String.format(Locale.ROOT, "frame-%04d.png", i)).toFile());
    }
    Path out = dir.resolve("video.webm");

    try {
      boolean stitched = VideoStitcher.stitch(ffmpeg, dir, 2, 320, 200, out);

      assertThat(stitched).isTrue();
      // Every WebM (Matroska) file starts with the EBML magic number 1A 45 DF A3.
      assertThat(Files.readAllBytes(out))
          .startsWith((byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3);
    } finally {
      try (Stream<Path> files = Files.list(dir)) {
        for (Path file : files.toList()) {
          Files.delete(file);
        }
      }
      Files.delete(dir);
    }
  }
}
