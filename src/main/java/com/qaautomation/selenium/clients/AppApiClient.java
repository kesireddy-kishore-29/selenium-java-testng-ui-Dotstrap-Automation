package com.qaautomation.selenium.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaautomation.selenium.core.Json;
import com.qaautomation.selenium.core.Redactor;
import java.io.IOException;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The UI flagship's self-hosted app (sut-ui) API, for seeding data around a browser assertion and
 * removing it afterwards. Every call has a timeout and checks its status, so a seeding failure is
 * reported as a seeding failure, not as a confusing UI failure. Tokens never appear in an error.
 */
public final class AppApiClient {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(3);

  /** An item as the API returns it. */
  public record Item(String id, String name, String status, String owner, String createdAt) {}

  private record ItemList(List<Item> items) {}

  private final URI base;
  private final HttpClient http;

  public AppApiClient(URI base) {
    this.base = base;
    this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  }

  public URI base() {
    return base;
  }

  /** Whether the app answers its health check; false when it is not running. */
  public boolean reachable() {
    try {
      HttpRequest health = request("/api/health", null).timeout(HEALTH_TIMEOUT).GET().build();
      return http.send(health, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  public void createUser(String email, String password) {
    expect(
        send(post("/api/users", Map.of("email", email, "password", password, "name", "QA"), null)),
        201);
  }

  public String token(String email, String password) {
    HttpResponse<String> response =
        send(post("/api/auth/token", Map.of("email", email, "password", password), null));
    expect(response, 200);
    return read(response.body(), JsonNode.class).path("token").asText();
  }

  /** The status a sign-in gets, whatever it is: 401 proves an account is gone. */
  public int signInStatus(String email, String password) {
    return send(post("/api/auth/token", Map.of("email", email, "password", password), null))
        .statusCode();
  }

  public Item createItem(String token, String name, String status) {
    HttpResponse<String> response =
        send(post("/api/items", Map.of("name", name, "status", status), token));
    expect(response, 201);
    return read(response.body(), Item.class);
  }

  public List<Item> items(String token) {
    HttpResponse<String> response = send(request("/api/items", token).GET().build());
    expect(response, 200);
    return read(response.body(), ItemList.class).items();
  }

  /** Deletes an item; an item that is already gone (404) is fine. */
  public void deleteItem(String token, String id) {
    expect(send(request("/api/items/" + id, token).DELETE().build()), 204, 404);
  }

  public void deleteMe(String token) {
    expect(send(request("/api/users/me", token).DELETE().build()), 204);
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new UncheckedIOException(describe(request) + " failed: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(describe(request) + " was interrupted");
    }
  }

  private static void expect(HttpResponse<String> response, int... allowed) {
    int status = response.statusCode();
    if (Arrays.stream(allowed).noneMatch(a -> a == status)) {
      throw new ApiException(
          describe(response.request())
              + " answered "
              + status
              + ", expected "
              + Arrays.toString(allowed)
              + ": "
              + Redactor.redact(response.body()));
    }
  }

  private HttpRequest post(String path, Object payload, String token) {
    return request(path, token)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(Json.toJson(payload)))
        .build();
  }

  private HttpRequest.Builder request(String path, String token) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(base.resolve(path))
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json");
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return builder;
  }

  private static String describe(HttpRequest request) {
    return request.method() + " " + request.uri().getPath();
  }

  private static <T> T read(String json, Class<T> type) {
    try {
      return Json.mapper().readValue(json, type);
    } catch (IOException e) {
      throw new UncheckedIOException("Unreadable response: " + Redactor.redact(json), e);
    }
  }

  /** A call answered with a status the caller did not expect. */
  public static final class ApiException extends IllegalStateException {

    @Serial private static final long serialVersionUID = 1L;

    public ApiException(String message) {
      super(message);
    }
  }
}
