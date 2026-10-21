package com.qaautomation.selenium.data;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads an amount out of display text: "$29.99", "Item total: $29.99", "Tax: $2.40". */
public final class Money {

  private static final Pattern AMOUNT = Pattern.compile("\\$([0-9]+(?:\\.[0-9]{2})?)");

  private Money() {}

  public static BigDecimal parse(String text) {
    Matcher matcher = AMOUNT.matcher(text);
    if (!matcher.find()) {
      throw new IllegalArgumentException("No amount in \"" + text + "\"");
    }
    return new BigDecimal(matcher.group(1));
  }
}
