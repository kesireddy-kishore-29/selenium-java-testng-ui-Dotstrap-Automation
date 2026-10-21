package com.qaautomation.selenium.core;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads Chrome's browser console and performance logs (enabled with {@code goog:loggingPrefs}).
 * Reading empties Chrome's buffer, so callers keep what they read. Network entries keep the method,
 * URL (redacted), status, MIME type and failure; headers are never kept.
 */
public final class BrowserLogs {

  private static final Logger LOG = LoggerFactory.getLogger(BrowserLogs.class);

  /** One request as the performance log saw it. */
  public record NetworkEntry(
      String requestId,
      String method,
      String url,
      String type,
      Integer status,
      String mimeType,
      String error) {}

  private BrowserLogs() {}

  public static List<String> console(WebDriver driver) {
    List<String> lines = new ArrayList<>();
    for (LogEntry entry : entries(driver, LogType.BROWSER)) {
      lines.add("[" + entry.getLevel() + "] " + Redactor.redact(entry.getMessage()));
    }
    return lines;
  }

  public static List<NetworkEntry> network(WebDriver driver) {
    Map<String, NetworkEntry> byId = new LinkedHashMap<>();
    for (LogEntry entry : entries(driver, LogType.PERFORMANCE)) {
      parse(entry.getMessage()).ifPresent(e -> byId.merge(e.requestId(), e, BrowserLogs::combine));
    }
    return new ArrayList<>(byId.values());
  }

  /** The status of the page's own document request, if the log saw it. */
  public static Optional<Integer> documentStatus(List<NetworkEntry> network, String url) {
    return network.stream()
        .filter(e -> "Document".equals(e.type()) && e.status() != null)
        .filter(e -> e.url() != null && stripSlash(e.url()).equals(stripSlash(url)))
        .map(NetworkEntry::status)
        .reduce((first, second) -> second);
  }

  static Optional<NetworkEntry> parse(String raw) {
    try {
      JsonNode message = Json.mapper().readTree(raw).path("message");
      String method = message.path("method").asText();
      JsonNode params = message.path("params");
      String id = params.path("requestId").asText(null);
      if (id == null) {
        return Optional.empty();
      }
      return switch (method) {
        case "Network.requestWillBeSent" ->
            Optional.of(
                new NetworkEntry(
                    id,
                    params.path("request").path("method").asText(null),
                    Redactor.redact(params.path("request").path("url").asText(null)),
                    params.path("type").asText(null),
                    null,
                    null,
                    null));
        case "Network.responseReceived" ->
            Optional.of(
                new NetworkEntry(
                    id,
                    null,
                    Redactor.redact(params.path("response").path("url").asText(null)),
                    params.path("type").asText(null),
                    params.path("response").path("status").asInt(),
                    params.path("response").path("mimeType").asText(null),
                    null));
        case "Network.loadingFailed" ->
            Optional.of(
                new NetworkEntry(
                    id,
                    null,
                    null,
                    params.path("type").asText(null),
                    null,
                    null,
                    params.path("errorText").asText(null)));
        default -> Optional.empty();
      };
    } catch (IOException e) {
      LOG.debug("Unreadable performance log entry: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private static NetworkEntry combine(NetworkEntry a, NetworkEntry b) {
    return new NetworkEntry(
        a.requestId(),
        first(a.method(), b.method()),
        first(a.url(), b.url()),
        first(a.type(), b.type()),
        first(b.status(), a.status()),
        first(b.mimeType(), a.mimeType()),
        first(b.error(), a.error()));
  }

  private static <T> T first(T preferred, T fallback) {
    return preferred != null ? preferred : fallback;
  }

  private static String stripSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static List<LogEntry> entries(WebDriver driver, String type) {
    try {
      return driver.manage().logs().get(type).getAll();
    } catch (WebDriverException e) {
      LOG.debug("No {} log from this driver: {}", type, e.getMessage());
      return List.of();
    }
  }
}
