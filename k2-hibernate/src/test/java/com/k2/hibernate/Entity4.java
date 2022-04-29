/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Embedded;
import javax.persistence.Id;

/** Sample entity to use in HibernateTest. */
@Entity
public class Entity4 {

  /** The pk. */
  private @Id @GeneratedValue long id;

  /** An embedded element to test @Prefix with an empty prefix. */
  @Embedded
  @Prefix(skip = true)
  private Value1 value1 = null;

  /** An embedded element to test @Prefix with a specific prefix. */
  @Embedded
  @Prefix("prefix")
  private Value1 attribute2 = null;

  /** An embedded element to test @Prefix with default values. */
  @Embedded
  @Prefix
  private Value1 attribute3 = null;

  /** The pk.
   *
   * @return the pk.
   */
  public long getId() {
    return id;
  }

  /** Sets value1.
   *
   * @param aValue the value 1
   */
  public void setValue1(final Value1 aValue) {
    value1 = aValue;
  }

  /** Gets the value1.
   *
   * @return the value1 or null.
   */
  public Value1 getValue1() {
    return value1;
  }

  /** Empty constructor. */
  Entity4() {
  }
}
