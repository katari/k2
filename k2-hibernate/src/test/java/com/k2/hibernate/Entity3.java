/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
