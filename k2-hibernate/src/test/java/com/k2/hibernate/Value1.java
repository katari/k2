/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Transient;

/** Sample value object to use in HibernateTest. */
@Embeddable
public class Value1 {

  /** an injected value. */
  @Transient private String injected;

  /** a sample column. */
  private String value;

  /** A sample nested embeddable. */
  @Embedded
  private Value2 attribute2;

  /** The value.
   *
   * @return the value.
   */
  public String getValue() {
    return value;
  }

  /** The injected value.
  *
  * @return the injected value.
  */
  public String getInjected() {
    return injected;
  }

  /** Empty constructor.
   *
   * @param theInjected the injected.
   */
  Value1(final String theInjected) {
    injected = theInjected;
  }

  /** Constructor to initialize the value.
   *
   * @param theInjected the injected.
   *
   * @param theValue the value.
   */
  Value1(final String theInjected, final String theValue) {
    injected = theInjected;
    value = theValue;
  }
}

