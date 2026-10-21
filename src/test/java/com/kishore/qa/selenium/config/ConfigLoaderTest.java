package com.kishore.qa.selenium.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.testng.annotations.Test;

public final class ConfigLoaderTest {

  private static Properties valid() {
    Properties p = new Properties();
    p.setProperty("sauce.baseUrl", "https://www.saucedemo.com");
    p.setProperty("sauce.username", "standard_user");
    p.setProperty("sauce.password", "secret_sauce");
    p.setProperty("app.baseUrl", "http://localhost:4173");
    p.setProperty("live.baseUrl", "https://dotstrap.com");
    p.setProperty("browser.headless", "true");
    p.setProperty("browser.width", "1366");
    p.setProperty("browser.height", "900");
    p.setProperty("wait.timeoutSeconds", "10");
    p.setProperty("wait.pageLoadSeconds", "30");
    p.setProperty("video.frameRate", "2");
    p.setProperty("run.id", "unit-1");
    return p;
  }

  @Test
  public void loadsAValidProfile() {
    Config config = ConfigLoader.load("local", valid(), Map.of(), new Properties());

    assertThat(config.sauceBaseUrl()).hasHost("www.saucedemo.com");
    assertThat(config.waitTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.recordMode()).isEqualTo(RecordMode.FAILURES);
    assertThat(config.videoMode()).isEqualTo(VideoMode.STITCH);
    assertThat(config.gridUrl()).isEmpty();
    assertThat(config.runId()).isEqualTo("unit-1");
  }

  @Test
  public void bundledProfilesAreValid() {
    for (String environment : new String[] {"local", "ci"}) {
      Config config =
          ConfigLoader.load(
              environment, ConfigLoader.readProfile(environment), Map.of(), new Properties());
      assertThat(config.environment()).isEqualTo(environment);
    }
  }

  @Test
  public void environmentVariablesOverrideTheFileAndSystemPropertiesOverrideBoth() {
    Properties system = new Properties();
    system.setProperty("wait.timeoutSeconds", "20");
    Map<String, String> env = Map.of("WAIT_TIMEOUT_SECONDS", "15", "RECORD_MODE", "all");

    Config config = ConfigLoader.load("local", valid(), env, system);

    assertThat(config.waitTimeout()).isEqualTo(Duration.ofSeconds(20));
    assertThat(config.recordMode()).isEqualTo(RecordMode.ALL);
  }

  @Test
  public void reportsEveryProblemAtOnce() {
    Properties broken = valid();
    broken.setProperty("sauce.baseUrl", "not a url");
    broken.setProperty("browser.headless", "maybe");
    broken.setProperty("browser.width", "99999");
    broken.remove("sauce.password");
    Map<String, String> env = Map.of("RECORD_MODE", "sometimes", "RUN_ID", "bad id!");

    assertThatThrownBy(() -> ConfigLoader.load("ci", broken, env, new Properties()))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("environment 'ci'")
        .hasMessageContaining("sauce.baseUrl")
        .hasMessageContaining("sauce.password is required (or set SAUCE_PASSWORD)")
        .hasMessageContaining("browser.headless must be true or false")
        .hasMessageContaining("browser.width must be between 320 and 3840")
        .hasMessageContaining("record.mode (RECORD_MODE) 'sometimes' is not one of")
        .hasMessageContaining("run.id (RUN_ID)");
  }

  @Test
  public void gridVideoNeedsAGridUrl() {
    assertThatThrownBy(
            () ->
                ConfigLoader.load("local", valid(), Map.of("VIDEO_MODE", "grid"), new Properties()))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("grid.url (GRID_URL) is required");

    Config config =
        ConfigLoader.load(
            "local",
            valid(),
            Map.of("VIDEO_MODE", "grid", "GRID_URL", "http://localhost:4444"),
            new Properties());
    assertThat(config.gridUrl()).isPresent();
  }

  @Test
  public void mapsPropertyKeysToEnvironmentVariableNames() {
    assertThat(ConfigLoader.envName("sauce.baseUrl")).isEqualTo("SAUCE_BASE_URL");
    assertThat(ConfigLoader.envName("wait.pageLoadSeconds")).isEqualTo("WAIT_PAGE_LOAD_SECONDS");
    assertThat(ConfigLoader.envName("record.mode")).isEqualTo("RECORD_MODE");
  }
}
