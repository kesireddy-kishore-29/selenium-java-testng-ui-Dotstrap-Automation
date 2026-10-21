package com.qaautomation.selenium.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.testng.annotations.Test;

public final class LivePagesTest {

  @Test
  public void theBundledPageListIsValid() {
    LivePages live = LivePages.load();

    assertThat(live.pages()).hasSizeGreaterThanOrEqualTo(3);
    assertThat(live.pages()).extracting(LivePages.Page::path).doesNotContain("/login");
    assertThat(live.widths()).containsExactly(1440, 1024, 768, 390);
    assertThat(live.page("/").headingPattern().matcher("ANYONE CAN KNOCK").find()).isTrue();
  }

  @Test
  public void everyMetadataAndResponsivePageIsAListedPage() {
    LivePages live = LivePages.load();
    List<String> listed = live.pages().stream().map(LivePages.Page::path).toList();

    assertThat(listed).containsAll(live.metadataPages()).containsAll(live.responsivePages());
  }

  @Test
  public void rejectsTheSignInPageBadPatternsAndOddWidthsAllAtOnce() {
    LivePages broken =
        new LivePages(
            List.of(
                new LivePages.Page("/login", "Sign in", "Welcome"),
                new LivePages.Page("/", "Home", "(unclosed")),
            List.of("pricing"),
            List.of(),
            List.of(200, 1440));

    assertThatThrownBy(() -> LivePages.validate(broken))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("config/live-pages.json")
        .hasMessageContaining("'/login' must start with / and must not be the sign-in page")
        .hasMessageContaining("/: heading is not a valid pattern")
        .hasMessageContaining("'pricing' must start with /")
        .hasMessageContaining("width 200 must be between 320 and 3840");
  }

  @Test
  public void anUnknownPageIsAnError() {
    assertThatThrownBy(() -> LivePages.load().page("/nowhere"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("/nowhere");
  }
}
