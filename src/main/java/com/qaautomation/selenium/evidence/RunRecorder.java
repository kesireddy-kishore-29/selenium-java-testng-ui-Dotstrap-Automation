package com.qaautomation.selenium.evidence;

import com.qaautomation.selenium.core.Json;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects every attempt of every test in a run and writes the documents shared by the family:
 * {@code run-summary.json} (the normalised result) and {@code flaky-report.json} (tests that
 * failed, then passed on a retry).
 */
public final class RunRecorder {

  private final RunInfo run;
  private final Instant startedAt = Instant.now();
  private final List<TestRecord> attempts = new ArrayList<>();

  public RunRecorder(RunInfo run) {
    this.run = run;
  }

  public synchronized void add(TestRecord record) {
    attempts.add(record);
  }

  public synchronized boolean isEmpty() {
    return attempts.isEmpty();
  }

  /** The last attempt of each test, with "flaky" when it passed after a failed attempt. */
  public synchronized List<TestRecord> finalResults() {
    Map<String, TestRecord> last = new LinkedHashMap<>();
    Map<String, Integer> count = new LinkedHashMap<>();
    for (TestRecord r : attempts) {
      count.merge(r.id(), 1, Integer::sum);
      TestRecord previous = last.get(r.id());
      if (previous == null || r.attempt() >= previous.attempt()) {
        last.put(r.id(), r);
      }
    }
    List<TestRecord> results = new ArrayList<>();
    for (TestRecord r : last.values()) {
      boolean flaky = "passed".equals(r.status()) && count.get(r.id()) > 1;
      results.add(flaky ? r.withStatus("flaky") : r);
    }
    results.sort(Comparator.comparing(TestRecord::suite).thenComparing(TestRecord::id));
    return results;
  }

  public synchronized List<TestRecord> attemptsOf(String testId) {
    return attempts.stream().filter(a -> a.id().equals(testId)).toList();
  }

  public RunInfo run() {
    return run;
  }

  public Instant startedAt() {
    return startedAt;
  }

  /** The headline numbers of a written summary. */
  public record Written(Map<String, Integer> totals, long durationMs) {}

  /** Writes run-summary.json and flaky-report.json; returns the totals that were written. */
  public Written write(Map<String, Object> environment) {
    List<TestRecord> results = finalResults();
    Instant finishedAt = Instant.now();
    Map<String, Integer> totals = new LinkedHashMap<>();
    totals.put("total", results.size());
    totals.put("passed", count(results, "passed"));
    totals.put("failed", count(results, "failed"));
    totals.put("skipped", count(results, "skipped"));
    totals.put("flaky", count(results, "flaky"));
    totals.put("quarantined", (int) results.stream().filter(TestRecord::quarantined).count());

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("schemaVersion", 1);
    summary.put("repo", run.repo());
    summary.put("runId", run.runId());
    summary.put("gitSha", run.gitSha());
    summary.put("recordMode", run.recordMode());
    summary.put("startedAt", startedAt);
    summary.put("finishedAt", finishedAt);
    summary.put("durationMs", Duration.between(startedAt, finishedAt).toMillis());
    summary.put("environment", environment);
    summary.put("totals", totals);
    summary.put("tests", results);

    List<Map<String, Object>> flaky = new ArrayList<>();
    for (TestRecord r : results) {
      if ("flaky".equals(r.status())) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("testId", r.id());
        entry.put("title", r.title());
        entry.put("featureIds", r.featureIds());
        entry.put("attempts", r.attempt());
        entry.put(
            "firstError",
            attemptsOf(r.id()).stream()
                .filter(a -> a.error() != null)
                .map(a -> a.error().type() + ": " + a.error().message())
                .findFirst()
                .orElse(""));
        flaky.add(entry);
      }
    }
    Map<String, Object> flakyReport = new LinkedHashMap<>();
    flakyReport.put("schemaVersion", 1);
    flakyReport.put("repo", run.repo());
    flakyReport.put("runId", run.runId());
    flakyReport.put("generatedAt", finishedAt);
    flakyReport.put("flaky", flaky);

    Json.write(run.runDir().resolve("run-summary.json"), summary);
    Json.write(run.runDir().resolve("flaky-report.json"), flakyReport);
    return new Written(totals, Duration.between(startedAt, finishedAt).toMillis());
  }

  private static int count(List<TestRecord> results, String status) {
    return (int) results.stream().filter(r -> status.equals(r.status())).count();
  }
}
