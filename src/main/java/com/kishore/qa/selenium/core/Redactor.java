package com.kishore.qa.selenium.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks secrets before evidence is written: credential and cookie headers written as text, token
 * and password fields in JSON, token query parameters and bearer tokens. Used on page sources,
 * console logs and network logs. Unit-tested.
 */
public final class Redactor {

  public static final String MASK = "****";

  private static final Pattern HEADER_LINE =
      Pattern.compile(
          "(?im)^(\\s*(?:authorization|proxy-authorization|cookie|set-cookie|x-api-key)\\s*:\\s*)"
              + "(?:(bearer|basic)\\s+)?\\S.*$");
  private static final Pattern JSON_SECRET =
      Pattern.compile(
          "(\"(?:password|token|accessToken|refreshToken|secret|apiKey)\"\\s*:\\s*\")[^\"]*(\")",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern QUERY_SECRET =
      Pattern.compile(
          "([?&](?:token|access_token|api_key|apikey|key|password)=)[^&#\\s\"']*",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern BEARER =
      Pattern.compile("(?i)\\b(bearer)\\s+[A-Za-z0-9._~+/=-]{8,}");
  private static final Pattern PASSWORD_VALUE =
      Pattern.compile(
          "(<input[^>]*type=\"password\"[^>]*value=\")[^\"]*(\")", Pattern.CASE_INSENSITIVE);

  private Redactor() {}

  public static String redact(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    String out = QUERY_SECRET.matcher(text).replaceAll("$1" + MASK);
    out = JSON_SECRET.matcher(out).replaceAll("$1" + MASK + "$2");
    out = BEARER.matcher(out).replaceAll("$1 " + MASK);
    out = PASSWORD_VALUE.matcher(out).replaceAll("$1" + MASK + "$2");
    Matcher header = HEADER_LINE.matcher(out);
    StringBuilder sb = new StringBuilder();
    while (header.find()) {
      String scheme = header.group(2);
      String replacement = header.group(1) + (scheme == null ? MASK : scheme + " " + MASK);
      header.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    header.appendTail(sb);
    return sb.toString();
  }
}
