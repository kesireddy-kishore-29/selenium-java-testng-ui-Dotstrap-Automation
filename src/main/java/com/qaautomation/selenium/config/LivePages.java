package com.qaautomation.selenium.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qaautomation.selenium.core.Json;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The live smoke's page list and expected text, from {@code config/live-pages.json}: the one file
 * to edit when the live site changes. Validated on load like the rest of the configuration.
 */
@JsonIgnoreProperties({"_comment"})
public record LivePages(
    List<Page> pages,
    List<String> metadataPages,
    List<String> responsivePages,
    List<Integer> widths) {

  public static final String RESOURCE = "config/live-pages.json";

  /** One public page: where it is, and what its title and first heading must say. */
  public record Page(String path, String title, String heading) {

    public Pattern titlePattern() {
      return Pattern.compile(title, Pattern.CASE_INSENSITIVE);
    }

    public Pattern headingPattern() {
      return Pattern.compile(heading, Pattern.CASE_INSENSITIVE);
    }
  }

  public static LivePages load() {
    try (InputStream in = LivePages.class.getClassLoader().getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw ConfigException.inFile(RESOURCE, List.of("not on the classpath"));
      }
      return validate(Json.mapper().readValue(in, LivePages.class));
    } catch (IOException e) {
      throw ConfigException.inFile(RESOURCE, List.of("unreadable: " + e.getMessage()));
    }
  }

  static LivePages validate(LivePages config) {
    List<String> problems = new ArrayList<>();
    if (config.pages() == null || config.pages().isEmpty()) {
      problems.add("pages must list at least one page");
    } else {
      for (Page page : config.pages()) {
        checkPath(page.path(), problems);
        checkPattern(page.path(), "title", page.title(), problems);
        checkPattern(page.path(), "heading", page.heading(), problems);
      }
    }
    for (String path : nullSafe(config.metadataPages())) {
      checkPath(path, problems);
    }
    for (String path : nullSafe(config.responsivePages())) {
      checkPath(path, problems);
    }
    for (Integer width : nullSafe(config.widths())) {
      if (width == null || width < 320 || width > 3840) {
        problems.add("width " + width + " must be between 320 and 3840");
      }
    }
    if (!problems.isEmpty()) {
      throw ConfigException.inFile(RESOURCE, problems);
    }
    return config;
  }

  public Page page(String path) {
    return pages.stream()
        .filter(p -> p.path().equals(path))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("No live page " + path + " in " + RESOURCE));
  }

  private static void checkPath(String path, List<String> problems) {
    if (path == null || !path.startsWith("/") || path.startsWith("/login")) {
      problems.add("path '" + path + "' must start with / and must not be the sign-in page");
    }
  }

  private static void checkPattern(String path, String field, String regex, List<String> problems) {
    if (regex == null || regex.isBlank()) {
      problems.add(path + ": " + field + " is required");
      return;
    }
    try {
      Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      problems.add(path + ": " + field + " is not a valid pattern: " + e.getDescription());
    }
  }

  private static <T> List<T> nullSafe(List<T> list) {
    return list == null ? List.of() : list;
  }
}
