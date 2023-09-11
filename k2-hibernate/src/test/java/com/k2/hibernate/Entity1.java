/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Sample entity to use in HibernateTest. */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {
    "this_is_a_long_attribute_name",
    "value"})})
public class Entity1 {

  /** The pk. */
  private @Id @GeneratedValue long id;

  /** a sample column. */
  private String value;

  /** a sample with a very long name to test unique index key length. */
  private String thisIsALongAttributeName;

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
  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Entity2> manyEntities = new ArrayList<>();

  /** A sample entity collection, to check generated fk with long names. */
  @ManyToMany
  private List<Entity2> thisIsAVeryLongAttributeNameToForceALongFkName = null;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
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

  /** Adds an Entity to the list of entities.
   *
   * @param entity to be added. Cannot be null.
   */
  public void addToManyEntities(final Entity2 entity) {
    manyEntities.add(entity);
  }

  /** Gets the entities lazily hold by this entity.
   *
   * @return a list, never null.
   */
  public List<Entity2> getManyEntities() {
    return manyEntities;
  }

  public void setOneEntity(final Entity2 theOneEntity) {
    oneEntity = theOneEntity;
  }

  public Entity2 getOneEntity() {
    return oneEntity;
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
