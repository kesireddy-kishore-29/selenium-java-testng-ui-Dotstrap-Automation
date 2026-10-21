package com.qaautomation.selenium.listeners;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qaautomation.selenium.core.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.testng.annotations.Test;

/**
 * The quarantine rule, the same in every repository of the family: a test in the {@code quarantine}
 * group must have an entry in {@code quarantine.json} with an owner, a reason and an expiry date
 * that has not passed, and every entry must point at a test that really is in the group.
 * Quarantined tests still run in their own non-gating job. Checked by a unit test.
 */
public final class QuarantinePolicy {

  public static final String GROUP = "quarantine";

  /** One entry of quarantine.json. {@code testId} is {@code fully.qualified.Class#method}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Entry(
      String testId, String title, String owner, String reason, String expires, String issue) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record File(Integer schemaVersion, List<Entry> entries) {}

  private QuarantinePolicy() {}

  public static List<Entry> read(Path file) {
    try {
      File parsed = Json.mapper().readValue(file.toFile(), File.class);
      return parsed.entries() == null ? List.of() : parsed.entries();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + file, e);
    }
  }

  /** Every problem with the entries against the tests that are in the group. */
  public static List<String> check(List<Entry> entries, List<String> tagged, LocalDate today) {
    List<String> problems = new ArrayList<>();
    for (Entry entry : entries) {
      String id = entry.testId();
      if (entry.owner() == null || entry.owner().isBlank()) {
        problems.add(id + ": no owner");
      }
      if (entry.reason() == null || entry.reason().isBlank()) {
        problems.add(id + ": no reason");
      }
      try {
        LocalDate expires = LocalDate.parse(String.valueOf(entry.expires()));
        if (expires.isBefore(today)) {
          problems.add(id + ": expired on " + expires + " (fix the test or renew with a reason)");
        }
      } catch (DateTimeParseException e) {
        problems.add(id + ": expires must be YYYY-MM-DD, was '" + entry.expires() + "'");
      }
      if (!tagged.contains(id)) {
        problems.add(
            id + ": listed in quarantine.json but the test is not in the quarantine group");
      }
    }
    for (String id : tagged) {
      if (entries.stream().noneMatch(e -> id.equals(e.testId()))) {
        problems.add(id + ": in the quarantine group but has no quarantine.json entry");
      }
    }
    return problems;
  }

  /** {@code Class#method} of every test method in {@code group} under a compiled classes folder. */
  public static List<String> taggedIn(Path classesDir, String packagePrefix, String group) {
    List<String> tagged = new ArrayList<>();
    try (Stream<Path> files = Files.walk(classesDir)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
        String name = classesDir.relativize(file).toString().replace('\\', '/').replace('/', '.');
        name = name.substring(0, name.length() - ".class".length());
        if (!name.startsWith(packagePrefix) || name.contains("$")) {
          continue;
        }
        Class<?> type = Class.forName(name, false, QuarantinePolicy.class.getClassLoader());
        for (Method method : type.getDeclaredMethods()) {
          Test test = method.getAnnotation(Test.class);
          if (test != null && Arrays.asList(test.groups()).contains(group)) {
            tagged.add(name + "#" + method.getName());
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot scan " + classesDir, e);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Compiled class not loadable: " + e.getMessage(), e);
    }
    return tagged;
  }
}
