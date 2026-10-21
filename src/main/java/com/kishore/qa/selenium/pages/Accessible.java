package com.kishore.qa.selenium.pages;

import org.openqa.selenium.WebElement;

/**
 * An element as assistive technology announces it: its computed ARIA role and accessible name, as
 * Chrome reports them ({@link WebElement#getAriaRole()}, {@link WebElement#getAccessibleName()}).
 * Lets a test assert "this is a button called Login", not only "this element exists".
 */
public record Accessible(String role, String name) {

  public static Accessible of(WebElement element) {
    return new Accessible(element.getAriaRole(), element.getAccessibleName());
  }
}
