package com.qaautomation.selenium.evidence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A compact Markdown summary of the run: totals, duration and the failed tests. Written next to the
 * run, and appended to the GitHub job summary when the run is in Actions.
 */
public final class StepSummary {

  private StepSummary() {}

  public static String render(RunInfo run, RunRecorder.Written summary, List<TestRecord> finals) {
    Map<String, Integer> t = summary.totals();
    long durationMs = summary.durationMs();
    StringBuilder md = new StringBuilder();
    md.append("## ").append(run.repo()).append(" · `").append(run.runId()).append("`\n\n");
    md.append("| Total | Passed | Failed | Flaky | Skipped | Quarantined | Duration |\n");
    md.append("|---|---|---|---|---|---|---|\n");
    md.append(
        String.format(
            Locale.ROOT,
            "| %d | %d | %d | %d | %d | %d | %.1f s |\n\n",
            t.get("total"),
            t.get("passed"),
            t.get("failed"),
            t.get("flaky"),
            t.get("skipped"),
            t.get("quarantined"),
            durationMs / 1000.0));
    List<TestRecord> failed = finals.stream().filter(r -> "failed".equals(r.status())).toList();
    if (!failed.isEmpty()) {
      md.append("<details><summary>").append(failed.size()).append(" failed test(s)</summary>\n\n");
      for (TestRecord f : failed) {
        String message = f.error() == null ? "" : f.error().message();
        String first = message.strip().lines().findFirst().orElse("");
        md.append("- `")
            .append(f.id())
            .append("`: ")
            .append(first.length() > 160 ? first.substring(0, 160) + "…" : first)
            .append('\n');
      }
      md.append("\n</details>\n\n");
    }
    md.append("Evidence: `run-evidence/")
        .append(run.repo())
        .append('/')
        .append(run.runId())
        .append("/` (gallery.html, failure-bundles/)\n");
    return md.toString();
  }

  public static void write(RunInfo run, RunRecorder.Written summary, List<TestRecord> finals) {
    String md = render(run, summary, finals);
    try {
      Files.writeString(run.runDir().resolve("summary.md"), md, StandardCharsets.UTF_8);
      String github = System.getenv("GITHUB_STEP_SUMMARY");
      if (github != null && !github.isBlank()) {
        Files.writeString(
            Path.of(github),
            md,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
