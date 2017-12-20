/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class SubClass2 extends BaseClass {

  /** Constructor to initialize the value.
   */
  SubClass2() {
    super("I am SubClass2");
  }
}

