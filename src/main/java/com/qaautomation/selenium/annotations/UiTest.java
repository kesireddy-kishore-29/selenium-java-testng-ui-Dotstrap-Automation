package com.qaautomation.selenium.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class whose methods each get their own browser, started before the method and quit
 * after its evidence is captured (composition through a listener, not a base class).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UiTest {

  /** True for the live smoke: always a local Chrome, and a user agent that names this project. */
  boolean live() default false;
}
