/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class TablePerClassSubClass2 extends TablePerClassBaseClass {

  /** Constructor to initialize the value.
   */
  TablePerClassSubClass2() {
    super("I am TablePerClassSubClass2");
  }
}

