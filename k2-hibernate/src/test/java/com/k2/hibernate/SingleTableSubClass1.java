/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class SingleTableSubClass1 extends SingleTableBaseClass {

  /** Constructor to initialize the value.
   */
  SingleTableSubClass1() {
    super("I am SingleTableSubClass1");
  }
}

