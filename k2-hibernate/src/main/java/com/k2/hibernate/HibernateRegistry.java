/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.AttributeConverter;

import java.util.HashMap;

import org.apache.commons.lang3.Validate;

import com.k2.core.ModuleDefinition;

/** The hibernate registry.
 *
 * This is used by other modules to register their persistent classes and
 * optional factories in the hibernate module.
 *
 * If a module wants hibernate to manage its persistent classes it must
 * implement Registrator, and the addRegistration operation. In that operation,
 * the module asks the moduleContext for an instance of HibernateRegistry:
 *
 * moduleContext.get(HibernateRegistry.class)
 *     .registerPersistentClass(Entity1.class);
 *
 * A module may also want to inject spring dependencies into its entities. For
 * that, create a class that contains a parameterless create operation, and
 * register it as a bean in the module. Then, in addRegistration, call:
 *
 * moduleContext.get(HibernateRegistry.class)
 *     .registerPersistentClass(Entity1.class, Entity2Factory.clas);
 *
 * Hibernate will look for a bean of type Entity2Factory and call the create()
 * operation to instantiate Entity1. This lets you initialize your factory
 * with spring, and use a package access constructor to create the entity
 * and pass the dependencies to it.
 */
public class HibernateRegistry {

  /** The definition of the module that is registering its persistent classes.
   *
   * This is never null.
   */
  private ModuleDefinition requestor;

  /** All the persistent classes registered by the requestor.
   *
   * This is never null.
   */
  private List<Class<?>> persistentClasses = new LinkedList<>();

  /** The factories to create instances of the registered persistent classes.
   *
   * Not all persistent classes will have a factory here. If a persistent
   * class does not have a factory, hibernate will use the standard method
   * for instantiating entities.
   *
   * Hibernate looks for an operation named 'create' with no arguments.
   *
   * This is never null.
   */
  private Map<Class<?>, Class<?>> factories = new HashMap<>();

  /** The attribute converters that converts entity attribute state
   * into the database column representation and back again.
   *
   * This is never null.
   */
  private List<Class<? extends AttributeConverter<?, ?>>> converters
      = new LinkedList<>();

  /** Constructor, creates a hibernate registry.
   *
   * @param theRequestor the definition of the module registering persistent
   * classes. It cannot be null.
   */
  HibernateRegistry(final ModuleDefinition theRequestor) {
    Validate.notNull(theRequestor, "The requestor cannot be null.");
    requestor = theRequestor;
  }

  /** Registers a persistent class or component that does not need a custom
   * factory.
   *
   * @param theClass the persistent class. It cannot be null.
   */
  public void registerPersistentClass(final Class<?> theClass) {
    Validate.notNull(theClass, "The class cannot be null.");
    persistentClasses.add(theClass);
  }

  /** Registers a persistent class or component with the factory that hibernate
   * will use to instantiate that persistent class.
   *
   * To instantiate a persistent class, hibernate will look for a bean instance
   * in the module application context of the type of the factory.
   *
   * @param theClass the persistent class. It cannot be null.
   *
   * @param factory the class of the entity factory. It cannot be null.
   */
  public void registerPersistentClass(final Class<?> theClass,
      final Class<?> factory) {
    persistentClasses.add(theClass);
    factories.put(theClass, factory);
  }

  /** Registers an attribute converter that converts entity attribute state
   * into the database column representation and back again.
   *
   * See AttributeConverter on ways to use this feature.
   *
   * @param converter the converter to register. It cannot be null.
   */
  public void registerConverter(
      final Class<? extends AttributeConverter<?, ?>> converter) {
    converters.add(converter);
  }

  /** Returns the list of persistent classes.
   *
   * @return the persistent classes, never returns null.
   */
  List<Class<?>> getPersistentClasses() {
    return persistentClasses;
  }

  /** Returns the list of converters.
   *
   * @return the converters, never returns null.
   */
  List<Class<? extends AttributeConverter<?, ?>>> getConverters() {
    return converters;
  }

  /** Returns the factory to create a new instance of persistentClass.
   *
   * @param persistentClass the type of the instance to create. It cannot be
   * null.
   *
   * @return the factory instance, or null if the persistent class was
   * registered without a factory.
   */
  Object getFactoryFor(final Class<?> persistentClass) {
    Class<?> factoryType = factories.get(persistentClass);
    if (factoryType != null) {
      return requestor.getBean(factoryType);
    }
    return null;
  }

  /** Returns the prefix to use to create tables in the database that
   * will store the persisent instances of the module.
   *
   * @return a string with the table prefix, never null.
   */
  String getRequestorPrefix() {
    String prefix = requestor.getModuleShortName();
    if (prefix == null) {
      prefix = requestor.getModuleName();
    }
    return prefix;
  }
}

