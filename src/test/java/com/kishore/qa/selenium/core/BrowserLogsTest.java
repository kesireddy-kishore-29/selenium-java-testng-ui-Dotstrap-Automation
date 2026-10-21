package com.kishore.qa.selenium.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

public final class BrowserLogsTest {

  private static String sent(String id, String url, String type) {
    return """
        {"message":{"method":"Network.requestWillBeSent","params":{"requestId":"%s",\
        "type":"%s","request":{"method":"GET","url":"%s"}}}}"""
        .formatted(id, type, url);
  }

  private static String received(String id, String url, String type, int status) {
    return """
        {"message":{"method":"Network.responseReceived","params":{"requestId":"%s",\
        "type":"%s","response":{"url":"%s","status":%d,"mimeType":"text/html"}}}}"""
        .formatted(id, type, url, status);
  }

  @Test
  public void readsRequestsAndResponsesAndIgnoresOtherEvents() {
    assertThat(BrowserLogs.parse(sent("1", "https://dotstrap.com/", "Document")))
        .hasValueSatisfying(
            e -> {
              assertThat(e.method()).isEqualTo("GET");
              assertThat(e.status()).isNull();
            });
    assertThat(BrowserLogs.parse(received("1", "https://dotstrap.com/", "Document", 200)))
        .hasValueSatisfying(e -> assertThat(e.status()).isEqualTo(200));
    assertThat(BrowserLogs.parse("{\"message\":{\"method\":\"Page.loadEventFired\"}}")).isEmpty();
    assertThat(BrowserLogs.parse("not json")).isEmpty();
  }

  @Test
  public void redactsTokensInLoggedUrls() {
    assertThat(BrowserLogs.parse(sent("2", "https://x.test/a?token=abc123", "XHR")))
        .hasValueSatisfying(e -> assertThat(e.url()).isEqualTo("https://x.test/a?token=****"));
  }

  @Test
  public void documentStatusIsTheLastDocumentResponseForTheUrlIgnoringATrailingSlash() {
    List<BrowserLogs.NetworkEntry> network =
        List.of(
            new BrowserLogs.NetworkEntry(
                "1", "GET", "https://dotstrap.com/pricing/", "Document", 200, "text/html", null),
            new BrowserLogs.NetworkEntry(
                "2", "GET", "https://dotstrap.com/app.js", "Script", 404, "text/plain", null));

    assertThat(BrowserLogs.documentStatus(network, "https://dotstrap.com/pricing")).contains(200);
    assertThat(BrowserLogs.documentStatus(network, "https://dotstrap.com/faq")).isEmpty();
  }
}
