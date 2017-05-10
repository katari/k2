/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/** Sample entity to use in HibernateTest. */
@Entity
public class Entity1 {

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

  /** Empty constructor. */
  Entity1() {
  }

  /** Constructor to initialize the value.
   *
   * @param theValue the value.
   */
  Entity1(final String theValue) {
    value = theValue;
  }
}
