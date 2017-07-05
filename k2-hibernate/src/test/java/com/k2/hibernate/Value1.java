/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Embeddable;

/** Sample value object to use in HibernateTest. */
@Embeddable
public class Value1 {

  /** a sample column. */
  private String value;

  /** The value.
   *
   * @return the value.
   */
  public String getValue() {
    return value;
  }

  /** Empty constructor. */
  Value1() {
  }

  /** Constructor to initialize the value.
   *
   * @param theValue the value.
   */
  Value1(final String theValue) {
    value = theValue;
  }
}

