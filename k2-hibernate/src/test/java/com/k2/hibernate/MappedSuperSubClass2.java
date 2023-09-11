/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import jakarta.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class MappedSuperSubClass2 extends MappedSuperBaseClass {

  /** Constructor to initialize the value.
   */
  MappedSuperSubClass2() {
    super("I am MappedSuperSubClass2");
  }
}

