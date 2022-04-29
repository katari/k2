/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
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

  /** A sample element collection, to check generated fk names and component
   * tuplizers. */
  @ElementCollection(fetch = FetchType.EAGER)
  private List<Value2> value2List = new LinkedList<>();

  /** An embedded element to test component tuplizers. */
  @Embedded
  private Value1 attribute1 = null;

  /** A sample entity collection, to check generated fk names. */
  @ManyToMany
  private List<Entity2> manyEntities = null;

  @ManyToOne
  @JoinColumn(foreignKey = @ForeignKey(name="fk_entity_1_one"))
  private Entity2 oneEntity = null;

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

  /** Adds a value to the list of values.
   *
   * @param someValue a sample value.
   */
  public void addValue2(final Value2 someValue) {
    value2List.add(someValue);
  }

  /** Sets value1.
   *
   * @param aValue the value 1
   */
  public void setAttribute1(final Value1 aValue) {
    attribute1 = aValue;
  }

  /** Obtains the list of values.
   *
   * @return a non-null list.
   */
  public List<Value2> getValue2List() {
    return value2List;
  }

  /** Gets the value1.
   *
   * @return the value1 or null.
   */
  public Value1 getAttribute1() {
    return attribute1;
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
