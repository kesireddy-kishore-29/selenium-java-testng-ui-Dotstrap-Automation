package com.qaautomation.selenium.tests.live;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaautomation.selenium.annotations.Feature;
import com.qaautomation.selenium.annotations.UiTest;
import com.qaautomation.selenium.config.LivePages;
import com.qaautomation.selenium.core.DriverFactory;
import com.qaautomation.selenium.pages.Accessible;
import com.qaautomation.selenium.pages.live.PublicPage;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Read-only smoke of the author's own production site, dotstrap.com: public pages only, no sign-in,
 * no forms, no writes, one browser at a time, and a user agent that names this project. Runs
 * nightly and on demand, never on pull requests, and never gates a merge. The pages and the text
 * they must show live in one file, config/live-pages.json.
 */
@UiTest(live = true)
public final class PublicSiteTest {

  private static final LivePages LIVE = LivePages.load();

  @DataProvider(name = "pages")
  public Object[][] pages() {
    return LIVE.pages().stream().map(p -> new Object[] {p.path()}).toArray(Object[][]::new);
  }

  @Test(
      dataProvider = "pages",
      groups = "live",
      description = "a public page answers 200 and renders its heading")
  @Feature("FEAT-LIVE-01")
  public void pageAnswers200WithItsHeading(String path) {
    LivePages.Page expected = LIVE.page(path);

    PublicPage page = new PublicPage(DriverFactory.driver()).open(path);

    assertThat(page.documentStatus()).as("document status of " + path).contains(200);
    assertThat(page.title()).as("title").containsPattern(expected.titlePattern());
    Accessible heading = page.heading();
    assertThat(heading.role()).as("first h1 role").isEqualTo("heading");
    assertThat(heading.name()).as("first heading").containsPattern(expected.headingPattern());
    assertThat(page.userAgent()).endsWith(DriverFactory.LIVE_USER_AGENT_SUFFIX);
  }

  @Test(groups = "live", description = "no public page logs a console error or a failed request")
  @Feature("FEAT-LIVE-02")
  public void noConsoleErrorsOrFailedRequests() {
    PublicPage page = new PublicPage(DriverFactory.driver());
    for (LivePages.Page live : LIVE.pages()) {
      page.open(live.path()).heading();
    }

    assertThat(page.consoleErrors()).as("console errors on any public page").isEmpty();
    assertThat(page.failedRequests()).as("failed requests on any public page").isEmpty();
  }

  @DataProvider(name = "metadataPages")
  public Object[][] metadataPages() {
    return LIVE.metadataPages().stream().map(p -> new Object[] {p}).toArray(Object[][]::new);
  }

  @Test(
      dataProvider = "metadataPages",
      groups = "live",
      description = "a public page has a title, a description and a canonical URL")
  @Feature("FEAT-LIVE-04")
  public void pageHasTitleDescriptionAndCanonical(String path) {
    PublicPage page = new PublicPage(DriverFactory.driver()).open(path);

    assertThat(page.title()).as("title").matches(".*\\S{3,}.*");
    assertThat(page.metaDescription())
        .as("meta description")
        .hasValueSatisfying(d -> assertThat(d.length()).as("description length").isGreaterThan(50));
    assertThat(page.canonical())
        .as("canonical")
        .hasValueSatisfying(c -> assertThat(c).startsWith("https://dotstrap.com"));
  }

  @DataProvider(name = "widths")
  public Object[][] widths() {
    return LIVE.widths().stream().map(w -> new Object[] {w}).toArray(Object[][]::new);
  }

  @Test(
      dataProvider = "widths",
      groups = "live",
      description = "the responsive pages have no horizontal overflow at this width")
  @Feature("FEAT-LIVE-05")
  public void noHorizontalOverflow(Integer width) {
    PublicPage page = new PublicPage(DriverFactory.driver()).viewport(width, 900);
    SoftAssertions softly = new SoftAssertions();
    for (String path : LIVE.responsivePages()) {
      PublicPage.Overflow overflow = page.open(path).horizontalOverflow();
      softly
          .assertThat(page.innerWidth())
          .as("%s: the browser really is %d px wide", path, width)
          .isEqualTo(width.longValue());
      softly
          .assertThat(overflow.documentWidth())
          .as("%s at %d px is wider than its viewport: %s", path, width, overflow.offenders())
          .isLessThanOrEqualTo(overflow.viewportWidth());
    }
    softly.assertAll();
  }
}
