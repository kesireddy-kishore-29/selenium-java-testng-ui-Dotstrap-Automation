package com.kishore.qa.selenium.evidence;

import java.time.Instant;
import java.util.List;

/** One finished attempt of one test, as written to meta.json and run-summary.json. */
public record TestRecord(
    String id,
    String title,
    String suite,
    List<String> featureIds,
    List<String> tags,
    String status,
    long durationMs,
    int attempt,
    boolean quarantined,
    Instant startedAt,
    Instant finishedAt,
    ErrorInfo error,
    EvidenceInfo evidence) {

  /** The failure, if any; {@code failingLocator} is the locator Selenium could not find. */
  public record ErrorInfo(String type, String message, String stack, String failingLocator) {}

  /** Where this attempt's evidence lives, relative to the run directory, and what it holds. */
  public record EvidenceInfo(String dir, List<String> files) {}

  public TestRecord withStatus(String newStatus) {
    return new TestRecord(
        id,
        title,
        suite,
        featureIds,
        tags,
        newStatus,
        durationMs,
        attempt,
        quarantined,
        startedAt,
        finishedAt,
        error,
        evidence);
  }
}
