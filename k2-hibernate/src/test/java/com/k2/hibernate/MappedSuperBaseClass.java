/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/** Sample entity to use in HibernateTest.
 *
 * This is the base class of a single table inherintance hierarchy. */
@MappedSuperclass
public class MappedSuperBaseClass {

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
  MappedSuperBaseClass() {
  }

  /** Constructor to initialize the value.
   *
   * @param theValue the value.
   */
  MappedSuperBaseClass(final String theValue) {
    value = theValue;
  }
}

