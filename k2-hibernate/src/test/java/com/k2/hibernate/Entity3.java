/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Sample entity to use in HibernateTest. */
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"value", "entity_1_id"}),
        @UniqueConstraint(columnNames = {"unique_value"})
    },
    indexes = {
        @Index(columnList = "entity_1_id")
    })
public class Entity3 {

  /** The pk. */
  private @Id @GeneratedValue long id;

  /** a sample column. */
  @Column(unique = true)
  private String value;

  /** a sample unique column with camel case name. */
  @Column()
  private String uniqueValue;

  /** a sample link to other entity. */
  @ManyToOne
  @JoinColumn
  private Entity1 entity1;

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
  Entity3() {
  }

  /** Constructor to initialize the value.
   *
   * @param theValue the value.
   */
  Entity3(final String theValue) {
    value = theValue;
  }
}
