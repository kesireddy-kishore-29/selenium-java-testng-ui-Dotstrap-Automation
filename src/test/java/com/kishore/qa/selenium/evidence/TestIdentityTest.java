package com.kishore.qa.selenium.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kishore.qa.selenium.annotations.Feature;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.Test;

public final class TestIdentityTest {

  /** A fixture test class: no @Test, never collected. */
  static final class Fixture {
    @Feature("FEAT-X-01")
    public void dataDriven(Object row) {}

    public void plain() {}
  }

  private static Method method(String name) throws NoSuchMethodException {
    return name.equals("plain")
        ? Fixture.class.getMethod(name)
        : Fixture.class.getMethod(name, Object.class);
  }

  private static TestIdentity row(int index, Object value) throws NoSuchMethodException {
    return TestIdentity.of(
        Fixture.class,
        method("dataDriven"),
        new Object[] {value},
        index,
        "Data-driven",
        List.of("regression"));
  }

  @Test
  public void aDataProviderRowKeepsItsIdAcrossRunsWhateverItsValues() throws Exception {
    // Unique data and objects without value equality: their hash codes differ every run.
    TestIdentity firstRun = row(1, "qa-" + UUID.randomUUID());
    TestIdentity secondRun = row(1, new Object());

    assertThat(secondRun.testId())
        .isEqualTo(firstRun.testId())
        .isEqualTo(Fixture.class.getName() + "#dataDriven[1]");
    assertThat(secondRun.slug()).isEqualTo(firstRun.slug()).isEqualTo("fixture-data-driven-1");
  }

  @Test
  public void eachRowHasItsOwnIdAndTheValuesStayInTheTitle() throws Exception {
    assertThat(row(0, "same").testId()).isNotEqualTo(row(1, "same").testId());
    assertThat(row(0, "/pricing").title()).isEqualTo("Data-driven [/pricing]");
  }

  @Test
  public void aTestWithoutParametersHasNoSuffix() throws Exception {
    TestIdentity id =
        TestIdentity.of(Fixture.class, method("plain"), new Object[0], 0, "", List.of());

    assertThat(id.testId()).isEqualTo(Fixture.class.getName() + "#plain");
    assertThat(id.title()).isEqualTo("plain");
    assertThat(id.suite()).isEqualTo("evidence");
  }
}
