package com.kishore.qa.selenium.clients;

import com.kishore.qa.selenium.data.Unique;
import java.util.ArrayList;
import java.util.List;
import org.testng.SkipException;

/**
 * A throwaway sut-ui account for one test, with the items it seeds. {@link #close()} deletes the
 * items and the account and then proves they are gone; a cleanup that did not happen fails the test
 * instead of leaking data into the next one. Use with try-with-resources.
 */
public final class AppAccount implements AutoCloseable {

  private final AppApiClient api;
  private final String email;
  private final String password;
  private final String token;
  private final List<String> seededItems = new ArrayList<>();

  private AppAccount(AppApiClient api, String email, String password, String token) {
    this.api = api;
    this.email = email;
    this.password = password;
    this.token = token;
  }

  /**
   * Creates and signs in a new account. Skips the test (not fails it) when the app is not running:
   * it is a sibling repository's app, started separately.
   */
  public static AppAccount createOrSkip(AppApiClient api) {
    if (!api.reachable()) {
      throw new SkipException(
          "The UI flagship's sut-ui is not reachable at "
              + api.base()
              + " (start it with: cd ui-automation-framework && npm run sut)");
    }
    String email = Unique.email();
    String password = "pw-" + email;
    api.createUser(email, password);
    return new AppAccount(api, email, password, api.token(email, password));
  }

  /** The session token, for the browser cookie. Never logged. */
  public String token() {
    return token;
  }

  public AppApiClient.Item seedItem(String name, String status) {
    AppApiClient.Item item = api.createItem(token, name, status);
    seededItems.add(item.id());
    return item;
  }

  public List<AppApiClient.Item> items() {
    return api.items(token);
  }

  @Override
  public void close() {
    for (String id : seededItems) {
      api.deleteItem(token, id);
    }
    List<String> left =
        api.items(token).stream().map(AppApiClient.Item::id).filter(seededItems::contains).toList();
    if (!left.isEmpty()) {
      throw new IllegalStateException("Seeded items were not deleted: " + left);
    }
    api.deleteMe(token);
    int status = api.signInStatus(email, password);
    if (status != 401) {
      throw new IllegalStateException(
          "Test account still signs in after deletion (status " + status + ")");
    }
  }
}
