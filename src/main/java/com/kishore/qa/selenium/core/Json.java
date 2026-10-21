package com.kishore.qa.selenium.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** One Jackson configuration for every document the framework writes. */
public final class Json {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .enable(SerializationFeature.INDENT_OUTPUT)
          .setDefaultPropertyInclusion(
              JsonInclude.Value.construct(
                  JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

  private Json() {}

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  public static void write(Path file, Object value) {
    try {
      Files.createDirectories(file.toAbsolutePath().getParent());
      MAPPER.writeValue(file.toFile(), value);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write " + file, e);
    }
  }

  public static String toJson(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
