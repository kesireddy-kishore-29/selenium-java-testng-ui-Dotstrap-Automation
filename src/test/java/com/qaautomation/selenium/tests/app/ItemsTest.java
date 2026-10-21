package com.qaautomation.selenium.tests.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaautomation.selenium.annotations.Feature;
import com.qaautomation.selenium.annotations.UiTest;
import com.qaautomation.selenium.clients.AppAccount;
import com.qaautomation.selenium.clients.AppApiClient;
import com.qaautomation.selenium.clients.AppApiClient.Item;
import com.qaautomation.selenium.config.Config;
import com.qaautomation.selenium.core.DriverFactory;
import com.qaautomation.selenium.data.Unique;
import com.qaautomation.selenium.pages.Accessible;
import com.qaautomation.selenium.pages.app.ItemsPage;
import org.testng.annotations.Test;

/**
 * Hybrid: data seeded through the UI flagship app's API, checked and changed in the browser, and
 * checked again through the API. Skips when that app is not running (it lives in a sibling repo).
 */
@UiTest
public final class ItemsTest {

  @Test(
      groups = "regression",
      description = "items seeded through the API are listed; one deleted in the UI leaves the API")
  @Feature("FEAT-ITEMS-01")
  public void seededItemsAreListedAndDeletedThroughTheTable() {
    AppApiClient api = new AppApiClient(Config.get().appBaseUrl());
    try (AppAccount account = AppAccount.createOrSkip(api)) {
      Item keep = account.seedItem(Unique.name("keep"), "paused");
      Item remove = account.seedItem(Unique.name("remove"), "active");

      ItemsPage items = new ItemsPage(DriverFactory.driver()).openSignedIn(account.token());

      assertThat(items.table().cell(keep.name(), "Status")).isEqualTo("paused");
      assertThat(items.table().cell(keep.name(), "Owner")).isEqualTo("QA");
      assertThat(items.deleteControl(remove.name()))
          .isEqualTo(new Accessible("button", "Delete " + remove.name()));

      items.cancelDelete(remove.name()).delete(remove.name());

      assertThat(items.hasItem(remove.name())).isFalse();
      assertThat(items.hasItem(keep.name())).isTrue();
      assertThat(account.items())
          .extracting(Item::id)
          .contains(keep.id())
          .doesNotContain(remove.id());
    }
  }
}
