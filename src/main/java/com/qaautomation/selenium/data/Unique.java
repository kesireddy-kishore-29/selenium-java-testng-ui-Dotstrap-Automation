package com.qaautomation.selenium.data;

import java.util.UUID;

/**
 * Names nobody else's test will use. Every identity a test creates starts with {@code qa-}, so a
 * sweeper can find and remove anything a crashed run left behind.
 */
public final class Unique {

  private Unique() {}

  /** {@code qa-<label>-<8 hex>}. */
  public static String name(String label) {
    return "qa-" + label + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  /** An address on the reserved {@code .test} domain, which the sut-ui requires. */
  public static String email() {
    return name("user") + "@example.test";
  }
}
