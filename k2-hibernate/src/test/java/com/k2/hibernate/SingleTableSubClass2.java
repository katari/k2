/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import jakarta.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class SingleTableSubClass2 extends SingleTableBaseClass {

  /** Constructor to initialize the value.
   */
  SingleTableSubClass2() {
    super("I am SingleTableSubClass2");
  }
}

