package com.kishore.qa.selenium.listeners;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.testng.annotations.Test;

public final class QuarantinePolicyTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
  private static final String CART = "com.example.CartTest#badge";
  private static final Path TEST_CLASSES = Path.of("target", "test-classes");
  private static final String TESTS = "com.kishore.qa.selenium.tests";

  private static QuarantinePolicy.Entry entry(String owner, String reason, String expires) {
    return new QuarantinePolicy.Entry(CART, "badge", owner, reason, expires, "#12");
  }

  /** The gate itself: this repository's quarantine.json against its compiled tests. */
  @Test
  public void thisRepositorysQuarantineIsValid() {
    List<String> tagged = QuarantinePolicy.taggedIn(TEST_CLASSES, TESTS, QuarantinePolicy.GROUP);
    List<QuarantinePolicy.Entry> entries = QuarantinePolicy.read(Path.of("quarantine.json"));

    assertThat(QuarantinePolicy.check(entries, tagged, LocalDate.now())).isEmpty();
  }

  @Test
  public void theScannerFindsTestsByGroup() {
    assertThat(QuarantinePolicy.taggedIn(TEST_CLASSES, TESTS, "smoke"))
        .containsExactlyInAnyOrder(
            TESTS + ".sauce.LoginTest#validCredentialsReachTheInventory",
            TESTS + ".sauce.InventoryTest#listsEveryProductWithNameAndPrice",
            TESTS + ".sauce.CheckoutTest#fullCheckoutCompletes");
  }

  @Test
  public void aValidEntryForATaggedTestPasses() {
    assertThat(
            QuarantinePolicy.check(
                List.of(entry("kishore", "Sauce Demo badge lags", "2026-11-01")),
                List.of(CART),
                TODAY))
        .isEmpty();
  }

  @Test
  public void refusesOwnerlessReasonlessAndExpiredEntries() {
    assertThat(
            QuarantinePolicy.check(List.of(entry(" ", null, "2026-10-20")), List.of(CART), TODAY))
        .containsExactly(
            CART + ": no owner",
            CART + ": no reason",
            CART + ": expired on 2026-10-20 (fix the test or renew with a reason)");
  }

  @Test
  public void refusesAnEntryWithoutATestAndATestWithoutAnEntry() {
    assertThat(QuarantinePolicy.check(List.of(entry("kishore", "r", "soon")), List.of(), TODAY))
        .containsExactly(
            CART + ": expires must be YYYY-MM-DD, was 'soon'",
            CART + ": listed in quarantine.json but the test is not in the quarantine group");
    assertThat(QuarantinePolicy.check(List.of(), List.of(CART), TODAY))
        .containsExactly(CART + ": in the quarantine group but has no quarantine.json entry");
  }
}
