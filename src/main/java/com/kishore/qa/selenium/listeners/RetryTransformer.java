package com.kishore.qa.selenium.listeners;

import com.kishore.qa.selenium.annotations.UiTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * Gives every browser test the {@link RetryAnalyzer} without annotating each one. The framework's
 * own unit tests are never retried: a flaky unit test is a bug to fix, not to retry.
 */
public final class RetryTransformer implements IAnnotationTransformer {

  // TestNG declares this callback with raw Class and Constructor; the override must match it.
  @Override
  @SuppressWarnings("rawtypes")
  public void transform(
      ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
    Class<?> owner = testMethod != null ? testMethod.getDeclaringClass() : testClass;
    if (owner != null && owner.isAnnotationPresent(UiTest.class)) {
      annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
  }
}
