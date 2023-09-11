/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import com.k2.hibernate.HibernateTest.StringHolder;

/** Sample entity to use in HibernateTest. */
@Entity
public class Entity2 {

  /** A transient value. */
  @Transient private StringHolder parameter;

  /** The pk. */
  private @Id @GeneratedValue long id;

  /** a sample column. */
  private String value;

  /** An attribute integrated through an AttributeConverter */
  private Address address;

  /** An attribute integrated through an AttributeConverter */
  private Phone phone;

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

  /** The transient.
   *
   * @return the transient.
   */
  public StringHolder getParameter() {
    return parameter;
  }

  /** The address.
   *
   * @return the address.
   */
  public Address getAddress() {
    return address;
  }

  /** The phone.
   *
   * @return the phone.
   */
  public Phone getPhone() {
    return phone;
  }

  /** Empty constructor with transients only.
   *
   * @param param a transient.
   */
  Entity2(final StringHolder param) {
    parameter = param;
  }

  /** Constructor to initialize the value.
   *
   * Initializes the address to Corrientes 2122 and the phone to 11-555-5555.
   *
   * @param param a transient.
   *
   * @param theValue the value.
   */
  Entity2(final StringHolder param, final String theValue) {
    parameter = param;
    value = theValue;

    address = new Address("Corrientes", "2122");
    phone = new Phone("11", "555-5555");
  }

  /** Default constructor, required by hibernate when lazy-loading entities.
   */
  Entity2() {
  }
}
