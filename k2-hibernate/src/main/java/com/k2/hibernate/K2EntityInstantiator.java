package com.k2.hibernate;

import java.lang.reflect.Method;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.springframework.util.ReflectionUtils;

/** Custom Instantiator for entities.
 *
 * It uses a factory method instead of a default constructor when hibernate
 * retrieves objects from the db.
 *
 * This way, clients can retrieve objects with services injected.
 */
public class K2EntityInstantiator implements EntityInstantiator {

  /** The class of the component that will be instantiated. Never null. */
  private final Class<?> persistentClass;

  /** The hibernate registry in which the component was registered to.
   * Never null. */
  private final HibernateRegistry hibernateRegistry;

  /** The Factory that will handle instantiation through a factory method.
   * It's null until lazily initiated. */
  private Object factory;

  /** The factory method that will handle instantiation.
   * It's null until lazily initiated. */
  private Method factoryMethod;

  /** A flag to lazy-initialize instantiation and skip accessing the factory
   * if not initialized yet. */
  private boolean initialized;

  /** Constructor.
   *
   * @param thePersistentClass the class of the component that will be
   * instantiated. Cannot be null.
   *
   * @param theHibernateRegistry in which the components' factory was
   * registered to. Cannot be null.
   */
  public K2EntityInstantiator(final Class<?> thePersistentClass,
                              final HibernateRegistry theHibernateRegistry) {
    persistentClass = thePersistentClass;
    hibernateRegistry = theHibernateRegistry;
  }

  @Override
  public Object instantiate(
      final SessionFactoryImplementor theSessionFactoryImplementor) {
    if (factory == null) {
      // Lazy-init to delay the process until all factory beans are initialized.
      factory = hibernateRegistry.getFactoryFor(persistentClass);
      factoryMethod = ReflectionUtils.findMethod(factory.getClass(), "create");
      factoryMethod.setAccessible(true);
    }
    return ReflectionUtils.invokeMethod(factoryMethod, factory);
  }

  @Override
  public boolean canBeInstantiated() {
    // first call is forced to be false to avoid an early instantiation while
    // we don't have the factory beans yet.
    if (!initialized) {
      initialized = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean isInstance(
      final Object theO,
      final SessionFactoryImplementor theSessionFactoryImplementor) {
    return persistentClass.isInstance(theO);
  }

  @Override
  public boolean isSameClass(
      final Object theO,
      final SessionFactoryImplementor theSessionFactoryImplementor) {
    return theO.getClass().equals(persistentClass);
  }
}
