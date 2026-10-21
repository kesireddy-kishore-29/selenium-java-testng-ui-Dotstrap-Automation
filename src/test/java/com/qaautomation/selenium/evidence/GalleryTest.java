package com.qaautomation.selenium.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.testng.annotations.Test;

public final class GalleryTest {

  @Test
  public void fillsTheSharedTemplateWithTheRunData() {
    String html = Gallery.render(Map.of("repo", "selenium-java-ui-framework", "runId", "unit-1"));

    assertThat(html).startsWith("<!doctype html>").doesNotContain("/*__DATA__*/null");
    assertThat(html).contains("\"runId\" : \"unit-1\"");
  }

  @Test
  public void dataCannotCloseTheScriptOrBreakTheJavaScriptString() {
    String lineSeparator = String.valueOf((char) 0x2028);
    String html =
        Gallery.render(Map.of("title", "</script><script>alert(1)</script>" + lineSeparator));

    assertThat(html).doesNotContain("</script><script>alert(1)");
    assertThat(html).contains("\\u003c/script>\\u003cscript>alert(1)");
    assertThat(html).contains("\\u2028").doesNotContain(lineSeparator);
  }
}
