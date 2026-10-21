package com.kishore.qa.selenium.listeners;

import static org.assertj.core.api.Assertions.assertThat;

import com.kishore.qa.selenium.annotations.Feature;
import com.kishore.qa.selenium.annotations.UiTest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

public final class TraceabilityListenerTest {

  /** Fixture browser tests. Not TestNG tests themselves: no @Test, never collected. */
  @UiTest
  static final class Fixture {
    @Feature("FEAT-LOGIN-01")
    public void traced() {}

    public void untraced() {}

    @Feature({"FEAT-CART-01", "feat-cart-2"})
    public void malformed() {}
  }

  /** A class that is not a browser test is not checked. */
  static final class NotABrowserTest {
    public void helper() {}
  }

  private static List<Method> methods(Class<?> type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(m -> !m.isSynthetic()).toList();
  }

  @Test
  public void reportsEveryBrowserTestWithoutAValidFeatureId() {
    List<String> problems = TraceabilityListener.problems(methods(Fixture.class));

    assertThat(problems)
        .containsExactlyInAnyOrder(
            "Fixture#untraced has no @Feature",
            "Fixture#malformed has a malformed feature ID 'feat-cart-2'");
  }

  @Test
  public void ignoresClassesThatAreNotBrowserTests() {
    assertThat(TraceabilityListener.problems(methods(NotABrowserTest.class))).isEmpty();
  }

  @Test
  public void featureIdsFollowTheSharedFormat() {
    assertThat(TraceabilityListener.FEATURE_ID.matcher("FEAT-CHECKOUT-02").matches()).isTrue();
    assertThat(TraceabilityListener.FEATURE_ID.matcher("FEAT-CHECKOUT-2").matches()).isFalse();
    assertThat(TraceabilityListener.FEATURE_ID.matcher("CHECKOUT-02").matches()).isFalse();
  }

  @Test
  public void retryCapIsZeroToTwoAndDefaultsToTwo() {
    assertThat(RetryAnalyzer.parseRetries(null)).isEqualTo(2);
    assertThat(RetryAnalyzer.parseRetries("0")).isZero();
    assertThat(RetryAnalyzer.parseRetries("5")).isEqualTo(2);
    assertThat(RetryAnalyzer.parseRetries("-1")).isZero();
    assertThat(RetryAnalyzer.parseRetries("two")).isEqualTo(2);
  }
}
