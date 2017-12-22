/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.ElementCollection;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

/** Sample entity to use in HibernateTest. */
@Entity
public class Entity1 {

  /** The pk. */
  private @Id @GeneratedValue long id;

  /** a sample column. */
  private String value;

  /** A sample element collection, to check generated fk names. */
  @ElementCollection
  private List<Long> longs = null;

  /** A sample element collection, to check generated fk names. */
  @ElementCollection
  private List<Value1> values = null;

  /** A sample entity collection, to check generated fk names. */
  @ManyToMany
  private List<Entity2> entities = null;

  /** A sample entity collection of a class hierarchy. */
  @OneToMany
  @JoinColumn(nullable = false)
  private List<SingleTableBaseClass> abstractEntity = null;

  /** The pk.
   *
   * @return the pk.
   */
  public long getId() {
    return id;
  }

  /** The value.
   *
   * @return the value.
   */
  public String getValue() {
    return value;
  }

  /** Empty constructor. */
  Entity1() {
  }

  /** Constructor to initialize the value.
   *
   * @param theValue the value.
   */
  Entity1(final String theValue) {
    value = theValue;
  }
}
