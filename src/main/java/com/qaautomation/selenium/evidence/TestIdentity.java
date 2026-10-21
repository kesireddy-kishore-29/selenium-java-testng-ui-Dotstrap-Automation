package com.qaautomation.selenium.evidence;

import com.qaautomation.selenium.annotations.Feature;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.testng.ITestResult;

/**
 * Stable names for one test invocation: the ID used in reports and quarantine, the slug used in
 * evidence paths, the suite (last package segment), the feature IDs and the TestNG groups.
 *
 * <p>A data-driven invocation is named by its row in the data provider ({@code [0]}, {@code [1]}…),
 * never by its parameter values: those can hold unique test data, lambdas or enums whose hash codes
 * change from one run to the next, and the ID must stay the same across runs for quarantine and
 * flaky history to recognise the test. The values stay readable in the title.
 */
public record TestIdentity(
    String testId,
    String title,
    String suite,
    String slug,
    List<String> featureIds,
    List<String> groups) {

  public static TestIdentity of(ITestResult result) {
    return of(
        result.getTestClass().getRealClass(),
        result.getMethod().getConstructorOrMethod().getMethod(),
        result.getParameters(),
        result.getParameterIndex(),
        result.getMethod().getDescription(),
        List.of(result.getMethod().getGroups()));
  }

  static TestIdentity of(
      Class<?> testClass,
      Method method,
      Object[] parameters,
      int parameterIndex,
      String description,
      List<String> groups) {
    String packageName = testClass.getPackageName();
    String suite = packageName.substring(packageName.lastIndexOf('.') + 1);
    String parameterSuffix = parameters.length == 0 ? "" : "[" + parameterIndex + "]";
    String testId = testClass.getName() + "#" + method.getName() + parameterSuffix;
    String title =
        (description == null || description.isBlank() ? method.getName() : description)
            + (parameters.length == 0 ? "" : " " + Arrays.toString(parameters));
    Feature feature = method.getAnnotation(Feature.class);
    List<String> features = feature == null ? List.of() : List.of(feature.value());
    String slug =
        slugify(
            testClass.getSimpleName()
                + "-"
                + method.getName()
                + (parameterSuffix.isEmpty() ? "" : "-" + parameterSuffix));
    return new TestIdentity(testId, title, suite, slug, features, groups);
  }

  public static String slugify(String text) {
    String slug =
        text.replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
    return slug.length() > 90 ? slug.substring(0, 90).replaceAll("-+$", "") : slug;
  }
}
