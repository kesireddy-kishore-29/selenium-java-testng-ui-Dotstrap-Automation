package com.qaautomation.selenium.data;

/**
 * Who is checking out. A {@code null} field is left empty on the form (validation tests).
 *
 * @param label how a data-driven test names this case in reports
 */
public record Customer(String label, String firstName, String lastName, String postalCode) {

  /** A complete, valid customer. */
  public static Customer aCustomer() {
    return new Customer("valid customer", "Ada", "Lovelace", "560001");
  }

  public Customer without(String field) {
    return switch (field) {
      case "first name" -> new Customer("missing first name", null, lastName, postalCode);
      case "last name" -> new Customer("missing last name", firstName, null, postalCode);
      case "postal code" -> new Customer("missing postal code", firstName, lastName, null);
      default -> throw new IllegalArgumentException("No customer field '" + field + "'");
    };
  }

  /** The label only, so a report never lists form values as the test's name. */
  @Override
  public String toString() {
    return label;
  }
}
