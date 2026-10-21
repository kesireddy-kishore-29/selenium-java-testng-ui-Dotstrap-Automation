package com.kishore.qa.selenium.evidence;

import com.kishore.qa.selenium.annotations.Feature;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.testng.ITestResult;

/**
 * Stable names for one test invocation: the ID used in reports and quarantine, the slug used in
 * evidence paths, the suite (last package segment), the feature IDs and the TestNG groups.
 */
public record TestIdentity(
    String testId,
    String title,
    String suite,
    String slug,
    List<String> featureIds,
    List<String> groups) {

  public static TestIdentity of(ITestResult result) {
    Method method = result.getMethod().getConstructorOrMethod().getMethod();
    Class<?> testClass = result.getTestClass().getRealClass();
    String packageName = testClass.getPackageName();
    String suite = packageName.substring(packageName.lastIndexOf('.') + 1);
    Object[] parameters = result.getParameters();
    String parameterSuffix =
        parameters.length == 0
            ? ""
            : "[" + Integer.toHexString(Arrays.deepHashCode(parameters)) + "]";
    String testId = testClass.getName() + "#" + method.getName() + parameterSuffix;
    String description = result.getMethod().getDescription();
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
    return new TestIdentity(
        testId, title, suite, slug, features, List.of(result.getMethod().getGroups()));
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
