package com.k2.hibernate;

import jakarta.persistence.AttributeConverter;

public class Phone {

  private String countryCode;
  private String number;

  public Phone(final String theCountryCode, final String theNumber) {
    countryCode = theCountryCode;
    number = theNumber;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public String getNumber() {
    return number;
  }

  public static class Converter implements AttributeConverter<Phone, String> {

    @Override
    public String convertToDatabaseColumn(final Phone attribute) {
      if (attribute != null) {
        return attribute.getCountryCode() + "-" + attribute.getNumber();
      } else {
        return null;
      }
    }

    @Override
    public Phone convertToEntityAttribute(final String value) {
      if (value != null) {
        String[] parts = value.split("-", 2);
        return new Phone(parts[0], parts[1]);
      } else {
        return null;
      }
    }
  }
}
