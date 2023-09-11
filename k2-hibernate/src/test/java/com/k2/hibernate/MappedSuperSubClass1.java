/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import jakarta.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class MappedSuperSubClass1 extends MappedSuperBaseClass {

  /** Constructor to initialize the value.
   */
  MappedSuperSubClass1() {
    super("I am MappedSuperSubClass1");
  }
}

