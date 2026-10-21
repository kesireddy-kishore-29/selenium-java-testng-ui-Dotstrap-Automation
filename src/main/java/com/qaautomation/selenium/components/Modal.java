package com.qaautomation.selenium.components;

import com.qaautomation.selenium.core.WaitHelper;
import com.qaautomation.selenium.pages.Accessible;
import com.qaautomation.selenium.pages.Locators;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * A modal dialog. Opening it is the page's job; this component checks that what opened really is a
 * dialog with the expected accessible name, then presses one of its buttons by label.
 */
public final class Modal {

  private final WaitHelper wait;
  private final By root;
  private final String expectedName;

  public Modal(WaitHelper wait, By root, String expectedName) {
    this.wait = wait;
    this.root = root;
    this.expectedName = expectedName;
  }

  /** Waits for the dialog and returns its role and name as Chrome computes them. */
  public Accessible awaitOpen() {
    WebElement dialog = wait.visible(root);
    Accessible accessible = Accessible.of(dialog);
    if (!"dialog".equals(accessible.role()) || !expectedName.equals(accessible.name())) {
      throw new IllegalStateException(
          "Expected a dialog named '" + expectedName + "' at " + root + ", found " + accessible);
    }
    return accessible;
  }

  public void confirm(String label) {
    press(label);
    wait.gone(root);
  }

  public void cancel() {
    press("Cancel");
    wait.gone(root);
  }

  private void press(String label) {
    awaitOpen();
    WebElement button =
        wait.until(
            "a '" + label + "' button in " + root,
            d ->
                d.findElement(root).findElements(Locators.tag("button")).stream()
                    .filter(b -> label.equals(b.getText().strip()))
                    .findFirst()
                    .orElse(null));
    button.click();
  }
}
