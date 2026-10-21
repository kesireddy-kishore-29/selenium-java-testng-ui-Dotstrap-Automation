package com.kishore.qa.selenium.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.testng.annotations.Test;

/** Slugs, PNG sizes, failure-bundle READMEs and zips. */
public final class EvidenceFilesTest {

  @Test
  public void slugsAreLowerCaseHyphenatedAndBounded() {
    assertThat(TestIdentity.slugify("CheckoutTest-fullCheckoutCompletes"))
        .isEqualTo("checkout-test-full-checkout-completes");
    assertThat(TestIdentity.slugify("LoginTest-invalid-[1a2b]"))
        .isEqualTo("login-test-invalid-1a2b");
    assertThat(TestIdentity.slugify("A".repeat(200))).hasSizeLessThanOrEqualTo(90);
  }

  @Test
  public void readsAPngSizeFromItsHeader() throws IOException {
    Path png = Files.createTempFile("frame", ".png");
    ImageIO.write(new BufferedImage(1348, 748, BufferedImage.TYPE_INT_RGB), "png", png.toFile());

    assertThat(EvidenceCapture.pngSize(png))
        .hasValueSatisfying(s -> assertThat(s).containsExactly(1348, 748));
    Files.writeString(png, "not a png");
    assertThat(EvidenceCapture.pngSize(png)).isEmpty();
    Files.delete(png);
  }

  @Test
  public void aFailureBundleExplainsItselfAndZipsEveryAttempt() throws IOException {
    Path runDir = Files.createTempDirectory("run");
    Path attemptDir = runDir.resolve("demo/deliberate-failure-test-missing-element/1");
    Files.createDirectories(attemptDir);
    Files.writeString(attemptDir.resolve("console.log"), "(no console output)");
    Files.writeString(attemptDir.resolve("meta.json"), "{}");
    RunInfo run = new RunInfo("selenium-java-ui-framework", "unit-1", "abc123", runDir, "failures");
    TestRecord failed =
        new TestRecord(
            "DeliberateFailureTest#missingElement",
            "demo: a renamed test id is a missing element",
            "demo",
            List.of("FEAT-DEMO-01"),
            List.of("demo-failure"),
            "failed",
            900,
            1,
            false,
            Instant.EPOCH,
            Instant.EPOCH,
            new TestRecord.ErrorInfo(
                "org.openqa.selenium.NoSuchElementException",
                "no such element",
                "",
                "By.cssSelector: [data-test=\"cart-count\"]"),
            new TestRecord.EvidenceInfo(
                "demo/deliberate-failure-test-missing-element/1", List.of("console.log")));

    Path bundle = FailureBundles.build(run, failed, List.of(attemptDir));

    String readme = Files.readString(bundle.resolve("README.md"));
    assertThat(readme)
        .contains("# Failure bundle: demo: a renamed test id is a missing element")
        .contains("| Failing locator | `By.cssSelector: [data-test=\"cart-count\"]` |")
        .contains("| Features | FEAT-DEMO-01 |")
        .contains("`attempt-N/video.webm`");
    List<String> entries = new ArrayList<>();
    Path zip = bundle.resolveSibling("deliberate-failure-test-missing-element.zip");
    try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
      for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
        entries.add(e.getName());
      }
    }
    assertThat(entries)
        .containsExactlyInAnyOrder("README.md", "attempt-1/console.log", "attempt-1/meta.json");
  }
}
