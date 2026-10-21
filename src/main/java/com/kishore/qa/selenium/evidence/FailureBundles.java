package com.kishore.qa.selenium.evidence;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * One folder (and one zip) per failed test holding every attempt's evidence and a README that says
 * what each file is and how to open it. Lives at {@code <run>/failure-bundles/<test-slug>/}.
 */
public final class FailureBundles {

  private FailureBundles() {}

  public static Path build(RunInfo run, TestRecord finalAttempt, List<Path> attemptDirs) {
    String slug = Path.of(finalAttempt.evidence().dir()).getParent().getFileName().toString();
    Path bundle = run.runDir().resolve("failure-bundles").resolve(slug);
    try {
      Files.createDirectories(bundle);
      for (Path attempt : attemptDirs) {
        copyTree(attempt, bundle.resolve("attempt-" + attempt.getFileName()));
      }
      Files.writeString(
          bundle.resolve("README.md"),
          readme(run, finalAttempt, attemptDirs.size()),
          StandardCharsets.UTF_8);
      zip(bundle, bundle.resolveSibling(slug + ".zip"));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot build failure bundle " + bundle, e);
    }
    return bundle;
  }

  static String readme(RunInfo run, TestRecord test, int attempts) {
    String locator =
        test.error() == null || test.error().failingLocator() == null
            ? "(none reported)"
            : "`" + test.error().failingLocator() + "`";
    return """
        # Failure bundle: %s

        | | |
        |---|---|
        | Test | `%s` |
        | Features | %s |
        | Run | `%s` (git `%s`) |
        | Attempts | %d |
        | Failing locator | %s |
        | Error | `%s` |

        ## What is in here

        | File | What it is | How to open |
        |---|---|---|
        | `attempt-N/video.webm` | The session, stitched from a screenshot after every page action \
        (Selenium has no recorder) | Any browser or video player |
        | `attempt-N/screenshot.png` | The viewport when the test failed | An image viewer |
        | `attempt-N/fullpage.png` | The whole page when the test failed | An image viewer |
        | `attempt-N/page-source.html` | The DOM when the test failed (redacted) | A browser or editor |
        | `attempt-N/console.log` | The browser console (redacted) | A text editor |
        | `attempt-N/network-log.json` | Every request: method, URL, status, failure (no headers) \
        | A JSON viewer |
        | `attempt-N/meta.json` | Test identity, features, browser, environment, git SHA, notes \
        | A JSON viewer |

        Selenium has no trace viewer; the page source, console and network log replace it.
        """
        .formatted(
            test.title(),
            test.id(),
            String.join(", ", test.featureIds()),
            run.runId(),
            run.gitSha(),
            attempts,
            locator,
            test.error() == null ? "" : test.error().type());
  }

  public static void zip(Path source, Path zipFile) throws IOException {
    try (OutputStream out = Files.newOutputStream(zipFile);
        ZipOutputStream zip = new ZipOutputStream(out);
        Stream<Path> files = Files.walk(source)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        zip.putNextEntry(new ZipEntry(source.relativize(file).toString().replace('\\', '/')));
        Files.copy(file, zip);
        zip.closeEntry();
      }
    }
  }

  private static void copyTree(Path from, Path to) throws IOException {
    try (Stream<Path> files = Files.walk(from)) {
      for (Path file : files.toList()) {
        Path target = to.resolve(from.relativize(file).toString());
        if (Files.isDirectory(file)) {
          Files.createDirectories(target);
        } else {
          Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }
}
