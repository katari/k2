package com.k2.hibernate;

import javax.persistence.AttributeConverter;

public class Address {

  private String street;
  private String number;

  public Address(final String theStreet, final String theNumber) {
    street = theStreet;
    number = theNumber;
  }

  public String getStreet() {
    return street;
  }

  public String getNumber() {
    return number;
  }

  public static class Converter implements AttributeConverter<Address, String> {

    @Override
    public String convertToDatabaseColumn(final Address attribute) {
      if (attribute != null) {
        return attribute.getStreet() + "-" + attribute.getNumber();
      } else {
        return null;
      }
    }

    @Override
    public Address convertToEntityAttribute(final String value) {
      if (value != null) {
        String[] parts = value.split("-", 2);
        return new Address(parts[0], parts[1]);
      } else {
        return null;
      }
    }
  }
}
