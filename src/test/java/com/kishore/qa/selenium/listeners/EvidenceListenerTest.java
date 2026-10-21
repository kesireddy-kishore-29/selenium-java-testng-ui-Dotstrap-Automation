package com.kishore.qa.selenium.listeners;

import static org.assertj.core.api.Assertions.assertThat;

import com.kishore.qa.selenium.evidence.TestRecord;
import org.openqa.selenium.NoSuchElementException;
import org.testng.annotations.Test;

/** The failing-locator extraction and message clean-up, on messages copied from real runs. */
public final class EvidenceListenerTest {

  private static final String DRIVER_DETAILS =
      """
      Build info: version: '4.51.0', revision: '35c5fab'
      System info: os.name: 'Windows 11', os.arch: 'amd64', java.version: '21.0.12'
      Driver info: org.openqa.selenium.chrome.ChromeDriver
      Capabilities {browserName: chrome, chrome: {userDataDir: C:\\Users\\someone\\AppData\\Loca...}}
      Session ID: afa1185cd81f72f54aa4031a06d5799e""";

  @Test
  public void readsTheLocatorOfAWaitTimeoutWithoutTheRetryNote() {
    String message =
        "Expected condition failed: waiting 10 s for visibility of By.cssSelector: "
            + "[data-test=\"unread-count\"] (tried for 10 seconds with 500 milliseconds interval)";

    assertThat(EvidenceListener.failingLocator(message))
        .isEqualTo("By.cssSelector: [data-test=\"unread-count\"]");
  }

  @Test
  public void keepsParenthesesThatBelongToTheLocator() {
    String message =
        "waiting 10 s for visibility of By.cssSelector: "
            + "[data-test=\"add-to-cart-test.allthethings()-t-shirt-(red)\"]\r\n"
            + "(tried for 10 seconds with 500 milliseconds interval)";

    assertThat(EvidenceListener.failingLocator(message))
        .isEqualTo("By.cssSelector: [data-test=\"add-to-cart-test.allthethings()-t-shirt-(red)\"]");
  }

  @Test
  public void readsTheSelectorOfANoSuchElementErrorWithUnescapedQuotes() {
    String message =
        "no such element: Unable to locate element: "
            + "{\"method\":\"css selector\",\"selector\":\"[data-test=\"cart-count\"]\"}\n"
            + "  (Session info: chrome=154.0.8037.98)";

    assertThat(EvidenceListener.failingLocator(message))
        .isEqualTo("By.cssSelector: [data-test=\"cart-count\"]");
  }

  @Test
  public void anAssertionFailureHasNoLocator() {
    assertThat(EvidenceListener.failingLocator("expected: \"Product\" but was: \"Products\""))
        .isNull();
  }

  @Test
  public void dropsSeleniumsDriverDetailsButKeepsTheMessageAndStack() {
    String text =
        "no such element: Unable to locate element\n"
            + DRIVER_DETAILS
            + "\n\tat org.openqa.selenium.remote.ErrorCodec.decode(ErrorCodec.java:167)";

    String cleaned = EvidenceListener.withoutDriverDetails(text);

    assertThat(cleaned)
        .startsWith("no such element: Unable to locate element")
        .contains("\tat org.openqa.selenium.remote.ErrorCodec.decode")
        .doesNotContain("Build info")
        .doesNotContain("Capabilities")
        .doesNotContain("someone");
  }

  @Test
  public void errorInfoCarriesTypeCleanMessageAndLocator() {
    NoSuchElementException error =
        new NoSuchElementException(
            "no such element: {\"method\":\"css selector\",\"selector\":\"#menu\"}");

    TestRecord.ErrorInfo info = EvidenceListener.errorInfo(error);

    assertThat(info.type()).isEqualTo("org.openqa.selenium.NoSuchElementException");
    assertThat(info.failingLocator()).isEqualTo("By.cssSelector: #menu");
    assertThat(info.message()).doesNotContain("Build info");
    assertThat(info.stack()).contains("EvidenceListenerTest").doesNotContain("Build info");
  }
}
