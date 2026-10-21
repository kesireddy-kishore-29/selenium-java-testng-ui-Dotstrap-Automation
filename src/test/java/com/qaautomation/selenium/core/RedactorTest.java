package com.qaautomation.selenium.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public final class RedactorTest {

  @Test
  public void masksCredentialHeadersWrittenAsText() {
    String text =
        "Authorization: Bearer abc.def.ghi\nCookie: sutui_session=s3cr3t\nAccept: text/html";

    String redacted = Redactor.redact(text);

    assertThat(redacted)
        .contains("Authorization: Bearer ****")
        .contains("Cookie: ****")
        .contains("Accept: text/html")
        .doesNotContain("abc.def.ghi")
        .doesNotContain("s3cr3t");
  }

  @Test
  public void masksSecretFieldsInJson() {
    String json = "{\"email\":\"a@example.test\",\"password\":\"pw-123\",\"token\":\"t0k3n\"}";

    assertThat(Redactor.redact(json))
        .isEqualTo("{\"email\":\"a@example.test\",\"password\":\"****\",\"token\":\"****\"}");
  }

  @Test
  public void masksTokenQueryParametersAndBearerTokensInFreeText() {
    assertThat(Redactor.redact("GET https://x.test/submit?universe=U&token=abcdef123&b=2"))
        .isEqualTo("GET https://x.test/submit?universe=U&token=****&b=2");
    assertThat(Redactor.redact("sent bearer eyJhbGciOiJIUzI1NiJ9.e30.sig to the API"))
        .isEqualTo("sent bearer **** to the API");
  }

  @Test
  public void masksATypedPasswordInPageSource() {
    String html = "<input id=\"password\" type=\"password\" value=\"secret_sauce\">";

    assertThat(Redactor.redact(html))
        .isEqualTo("<input id=\"password\" type=\"password\" value=\"****\">");
  }

  @Test
  public void leavesOrdinaryTextAlone() {
    String text = "Products sorted by price; 6 items; a token of thanks";

    assertThat(Redactor.redact(text)).isEqualTo(text);
    assertThat(Redactor.redact(null)).isNull();
  }
}
