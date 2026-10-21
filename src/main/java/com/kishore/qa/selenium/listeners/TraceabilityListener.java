package com.kishore.qa.selenium.listeners;

import com.kishore.qa.selenium.annotations.Feature;
import com.kishore.qa.selenium.annotations.UiTest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Refuses to start a suite that contains a browser test without a valid {@link Feature} ID:
 * traceability is a build rule, not a convention, so an untraceable test never runs. The
 * framework's own unit tests are not browser tests and are not checked.
 */
public final class TraceabilityListener implements ISuiteListener {

  static final Pattern FEATURE_ID = Pattern.compile("FEAT-[A-Z0-9]+-\\d{2}");

  @Override
  public void onStart(ISuite suite) {
    List<String> problems =
        problems(
            suite.getAllMethods().stream()
                .map(m -> m.getConstructorOrMethod().getMethod())
                .toList());
    if (!problems.isEmpty()) {
      throw new IllegalStateException(
          "Traceability check failed; every browser test needs @Feature(\"FEAT-<AREA>-<NN>\"):\n  - "
              + String.join("\n  - ", problems));
    }
  }

  static List<String> problems(List<Method> methods) {
    List<String> problems = new ArrayList<>();
    for (Method method : methods) {
      if (!method.getDeclaringClass().isAnnotationPresent(UiTest.class)) {
        continue;
      }
      String name = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
      Feature feature = method.getAnnotation(Feature.class);
      if (feature == null || feature.value().length == 0) {
        problems.add(name + " has no @Feature");
        continue;
      }
      for (String id : feature.value()) {
        if (!FEATURE_ID.matcher(id).matches()) {
          problems.add(name + " has a malformed feature ID '" + id + "'");
        }
      }
    }
    return problems;
  }
}
