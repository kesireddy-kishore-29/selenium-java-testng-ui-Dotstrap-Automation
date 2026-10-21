package com.qaautomation.selenium.evidence;

import com.qaautomation.selenium.config.Config;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Identity of a run: repository, run ID, git SHA and where its evidence lives. */
public record RunInfo(String repo, String runId, String gitSha, Path runDir, String recordMode) {

  public static final String REPO = "selenium-java-ui-framework";

  public static RunInfo of(Config config) {
    return new RunInfo(
        REPO,
        config.runId(),
        detectGitSha(),
        config.evidenceRoot().resolve(REPO).resolve(config.runId()),
        config.recordMode().name().toLowerCase(Locale.ROOT));
  }

  private static String detectGitSha() {
    return Optional.ofNullable(System.getenv("GITHUB_SHA")).or(RunInfo::localSha).orElse("unknown");
  }

  private static Optional<String> localSha() {
    try {
      Process git = new ProcessBuilder("git", "rev-parse", "--short=12", "HEAD").start();
      if (!git.waitFor(5, TimeUnit.SECONDS) || git.exitValue() != 0) {
        git.destroyForcibly();
        return Optional.empty();
      }
      String sha = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
      return sha.isEmpty() ? Optional.empty() : Optional.of(sha);
    } catch (IOException e) {
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }
}
