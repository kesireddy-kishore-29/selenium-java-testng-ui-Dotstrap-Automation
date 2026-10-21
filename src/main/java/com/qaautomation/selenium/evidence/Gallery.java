package com.qaautomation.selenium.evidence;

import com.qaautomation.selenium.core.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The run gallery: every test of the run with its attempts, video, screenshot and evidence links.
 * The template is the family's shared gallery page, byte for byte; only the data differs.
 */
public final class Gallery {

  private static final String PLACEHOLDER = "/*__DATA__*/null";
  // A JavaScript string literal cannot contain these two characters raw, so the JSON escapes them.
  private static final String LINE_SEPARATOR = String.valueOf((char) 0x2028);
  private static final String PARAGRAPH_SEPARATOR = String.valueOf((char) 0x2029);

  private Gallery() {}

  public static Path write(RunInfo run, List<TestRecord> finals) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("repo", run.repo());
    data.put("runId", run.runId());
    data.put("recordMode", run.recordMode());
    data.put("base", "");
    List<Map<String, Object>> tests = new ArrayList<>();
    for (TestRecord t : finals) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("title", t.title());
      row.put("suite", t.suite());
      row.put("status", t.status());
      row.put("durationMs", t.durationMs());
      row.put("featureIds", t.featureIds());
      row.put("attempts", attempts(run, t));
      tests.add(row);
    }
    data.put("tests", tests);
    Path out = run.runDir().resolve("gallery.html");
    try {
      Files.writeString(out, render(data), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out;
  }

  static String render(Object data) {
    String html;
    try (InputStream in = Gallery.class.getResourceAsStream("/templates/gallery.html")) {
      html =
          new String(
              Objects.requireNonNull(in, "templates/gallery.html").readAllBytes(),
              StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    String json =
        Json.toJson(data)
            .replace("<", "\\u003c")
            .replace(LINE_SEPARATOR, "\\u2028")
            .replace(PARAGRAPH_SEPARATOR, "\\u2029");
    return html.replace(PLACEHOLDER, json);
  }

  private static List<Map<String, Object>> attempts(RunInfo run, TestRecord test) {
    List<Map<String, Object>> attempts = new ArrayList<>();
    if (test.evidence() == null) {
      return attempts;
    }
    Path testDir = run.runDir().resolve(test.evidence().dir()).getParent();
    try (Stream<Path> dirs = Files.list(testDir)) {
      for (Path dir : dirs.filter(Files::isDirectory).sorted().toList()) {
        Path meta = dir.resolve("meta.json");
        if (!Files.exists(meta)) {
          continue;
        }
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("attempt", Integer.parseInt(dir.getFileName().toString()));
        a.put("status", Json.mapper().readTree(meta.toFile()).path("test").path("status").asText());
        a.put("dir", run.runDir().relativize(dir).toString().replace('\\', '/'));
        try (Stream<Path> files = Files.list(dir)) {
          a.put("files", files.map(f -> f.getFileName().toString()).sorted().toList());
        }
        attempts.add(a);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return attempts;
  }
}
