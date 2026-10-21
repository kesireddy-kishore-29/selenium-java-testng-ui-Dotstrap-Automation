package com.kishore.qa.selenium.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

public final class RecordModeTest {

  @Test
  public void parsesTheThreeModesInAnyCase() {
    assertThat(RecordMode.parse("failures")).isEqualTo(RecordMode.FAILURES);
    assertThat(RecordMode.parse(" ALL ")).isEqualTo(RecordMode.ALL);
    assertThat(RecordMode.parse("Off")).isEqualTo(RecordMode.OFF);
  }

  @Test
  public void anUnknownModeNamesTheValidOnes() {
    assertThatThrownBy(() -> RecordMode.parse("everything"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("'everything' is not one of failures, all, off");
  }

  @Test
  public void keepsEvidenceAccordingToTheMode() {
    assertThat(RecordMode.FAILURES.keeps(true)).isTrue();
    assertThat(RecordMode.FAILURES.keeps(false)).isFalse();
    assertThat(RecordMode.ALL.keeps(false)).isTrue();
    assertThat(RecordMode.OFF.keeps(true)).isFalse();
    assertThat(RecordMode.OFF.recordsWhileRunning()).isFalse();
    assertThat(RecordMode.FAILURES.recordsWhileRunning()).isTrue();
  }

  @Test
  public void videoModeParsesAndRejects() {
    assertThat(VideoMode.parse("grid")).isEqualTo(VideoMode.GRID);
    assertThatThrownBy(() -> VideoMode.parse("vhs"))
        .hasMessage("'vhs' is not one of stitch, grid, off");
  }
}
