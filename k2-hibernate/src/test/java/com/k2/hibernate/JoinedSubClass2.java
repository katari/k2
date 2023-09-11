/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import jakarta.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class JoinedSubClass2 extends JoinedBaseClass {

  /** Constructor to initialize the value.
   */
  JoinedSubClass2() {
    super("I am JoinedSubClass2");
  }
}

