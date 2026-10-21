package com.qaautomation.selenium.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaautomation.selenium.core.Json;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

public final class RunRecorderTest {

  private static TestRecord attempt(String id, String status, int attempt, String error) {
    return new TestRecord(
        id,
        id,
        "sauce",
        List.of("FEAT-CART-01"),
        List.of("regression"),
        status,
        100,
        attempt,
        false,
        Instant.EPOCH,
        Instant.EPOCH,
        error == null
            ? null
            : new TestRecord.ErrorInfo("java.lang.AssertionError", error, "", null),
        null);
  }

  private static RunRecorder recorder(Path dir) {
    return new RunRecorder(
        new RunInfo("selenium-java-testng-ui-Dotstrap-Automation", "unit", "abc", dir, "all"));
  }

  @Test
  public void aTestThatPassesOnARetryIsFlakyNotPassed() {
    RunRecorder recorder = recorder(Path.of("unused"));
    recorder.add(attempt("A", "retried", 1, "badge showed 1"));
    recorder.add(attempt("A", "passed", 2, null));
    recorder.add(attempt("B", "passed", 1, null));
    recorder.add(attempt("C", "retried", 1, "boom"));
    recorder.add(attempt("C", "retried", 2, "boom"));
    recorder.add(attempt("C", "failed", 3, "boom"));

    assertThat(recorder.finalResults())
        .extracting(TestRecord::id, TestRecord::status, TestRecord::attempt)
        .containsExactly(tuple("A", "flaky", 2), tuple("B", "passed", 1), tuple("C", "failed", 3));
  }

  @Test
  public void writesTheSummaryAndTheFlakyReport() throws IOException {
    Path dir = Files.createTempDirectory("run");
    RunRecorder recorder = recorder(dir);
    recorder.add(attempt("A", "retried", 1, "badge showed 1"));
    recorder.add(attempt("A", "passed", 2, null));
    recorder.add(attempt("B", "skipped", 1, null));

    RunRecorder.Written written = recorder.write(Map.of("browser", "chrome"));

    assertThat(written.totals())
        .containsEntry("total", 2)
        .containsEntry("passed", 0)
        .containsEntry("flaky", 1)
        .containsEntry("skipped", 1)
        .containsEntry("failed", 0);
    JsonNode summary = Json.mapper().readTree(dir.resolve("run-summary.json").toFile());
    assertThat(summary.path("schemaVersion").asInt()).isEqualTo(1);
    assertThat(summary.path("tests")).hasSize(2);
    JsonNode flaky = Json.mapper().readTree(dir.resolve("flaky-report.json").toFile());
    assertThat(flaky.path("flaky")).hasSize(1);
    assertThat(flaky.path("flaky").get(0).path("testId").asText()).isEqualTo("A");
    assertThat(flaky.path("flaky").get(0).path("firstError").asText())
        .isEqualTo("java.lang.AssertionError: badge showed 1");
  }
}
