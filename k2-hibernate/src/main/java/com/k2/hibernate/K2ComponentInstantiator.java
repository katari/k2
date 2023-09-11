package com.k2.hibernate;

import java.lang.reflect.Method;

import org.apache.commons.lang3.Validate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.springframework.util.ReflectionUtils;

/** Custom Instantiator for embeddable components.
 *
 * It uses a factory method instead of a default constructor when hibernate
 * retrieves objects from the db.
 *
 * This way, clients can retrieve objects with services injected.
 */
public class K2ComponentInstantiator implements EmbeddableInstantiator {

  /** The class of the component that will be instantiated. Never null. */
  private final Class<?> componentClass;

  /** The hibernate registry in which the component was registered to.
   * Never null. */
  private final HibernateRegistry hibernateRegistry;

  /** The Factory that will handle instantiaton through a factory method.
   * It's null until lazily initiated. */
  private Object factory;

  /** The factory method that will handle instantiaton.
   * It's null until lazily initiated. */
  private Method factoryMethod;

  /** Constructor.
   *
   * @param theComponentClass the class of the component that will be
   * instantiated. Cannot be null.
   *
   * @param theHibernateRegistry in which the components' factory was
   * registered to. Cannot be null.
   */
  public K2ComponentInstantiator(final Class<?> theComponentClass,
                                 final HibernateRegistry theHibernateRegistry) {
    Validate.notNull(theComponentClass,
      "The ComponentClass cannot be null.");
    Validate.notNull(theHibernateRegistry,
      "The HibernateRegistry cannot be null.");
    componentClass = theComponentClass;
    hibernateRegistry = theHibernateRegistry;
  }

  @Override
  public Object instantiate(
      final ValueAccess valueAccess,
      final SessionFactoryImplementor sessionFactoryImpl) {
    if (factory == null) {
      // Lazy-init to delay the process until all factory beans are initialized.
      factory = hibernateRegistry.getFactoryFor(componentClass);
      factoryMethod = ReflectionUtils.findMethod(factory.getClass(), "create");
      factoryMethod.setAccessible(true);
    }
    return ReflectionUtils.invokeMethod(factoryMethod, factory);
  }

  @Override
  public boolean isInstance(
      final Object theO,
      final SessionFactoryImplementor theSessionFactoryImplementor) {
    return componentClass.isInstance(theO);
  }

  @Override
  public boolean isSameClass(
      final Object theO,
      final SessionFactoryImplementor theSessionFactoryImplementor) {
    return theO.getClass().equals(componentClass);
  }

}
