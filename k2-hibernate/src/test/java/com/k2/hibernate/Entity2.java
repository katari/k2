/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.k2.hibernate.HibernateTest.StringHolder;

/** Sample entity to use in HibernateTest. */
@Entity
public class Entity2 {

  /** A transient value. */
  @Transient private StringHolder parameter;

  /** The pk. */
  private @Id @GeneratedValue long id;

  /** a sample column. */
  private String value;

  /** The pk.
   *
   * @return the pk.
   */
  public long getId() {
    return id;
  }

  /** The value.
   *
   * @return the value.
   */
  public String getValue() {
    return value;
  }

  /** The transient.
   *
   * @return the transient.
   */
  public StringHolder getParameter() {
    return parameter;
  }

  /** Empty constructor with transients only.
   *
   * @param param a transient.
   */
  Entity2(final StringHolder param) {
    parameter = param;
  }

  /** Constructor to initialize the value.
   *
   * @param param a transient.
   *
   * @param theValue the value.
   */
  Entity2(final StringHolder param, final String theValue) {
    parameter = param;
    value = theValue;
  }
}
