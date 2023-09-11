/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import jakarta.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class TablePerClassSubClass1 extends TablePerClassBaseClass {

  /** Constructor to initialize the value.
   */
  TablePerClassSubClass1() {
    super("I am TablePerClassSubClass1");
  }
}

