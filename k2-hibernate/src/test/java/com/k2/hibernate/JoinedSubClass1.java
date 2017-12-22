/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Entity;

/** Sample entity to use in HibernateTest. */
@Entity
public class JoinedSubClass1 extends JoinedBaseClass {

  /** Constructor to initialize the value.
   */
  JoinedSubClass1() {
    super("I am JoinedSubClass1");
  }
}

