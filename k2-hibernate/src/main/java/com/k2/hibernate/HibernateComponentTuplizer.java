/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.List;

import java.lang.reflect.Method;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.util.ReflectionUtils;

import org.apache.commons.lang3.Validate;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;
import org.hibernate.mapping.Component;
import org.hibernate.tuple.PojoInstantiator;
import org.hibernate.tuple.component.PojoComponentTuplizer;

/** K2 hibernate tuplizer that uses module provided factories to create
 * component instances.
 *
 * See HibernateRegistry for more information.
 *
 * Note: this class is almost identical to HibernateEntityTuplizer. Look at
 * that class if you modify anything here.
 */
@SuppressWarnings("serial")
public class HibernateComponentTuplizer extends PojoComponentTuplizer {

  /** The list of registries with the entity/component classes and factories
   * provided by other modules, never null.
   */
  private List<HibernateRegistry> registries;

  /** A copy of the reflection optimizer obtained from the parent class, via
   * reflection, null if it is null in the parent class.
   */
  private ReflectionOptimizer reflectionOptimizer;

  /** Constructor, creates a hibernate tuplizer.
   *
   * @param component the component to instantian, as passed by
   * hibernate. This is never null.
   */
  public HibernateComponentTuplizer(final Component component) {
    super(component);

    registries = component.getServiceRegistry()
        .getService(Hibernate.HibernateRegistryLocator.class).getRegistries();

    // Hack to obtain the superclass configured reflection optimizer.
    DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(this);
    reflectionOptimizer = (ReflectionOptimizer) fieldAccessor.getPropertyValue(
        "optimizer");
  }

  /** Overriden to use our own instantiator (see Instantiator).
   *
   * {@inheritDoc}.*/
  @Override
  protected Instantiator buildInstantiator(final Component component) {
    InstantiationOptimizer optimizer = null;
    if (reflectionOptimizer != null) {
      optimizer = reflectionOptimizer.getInstantiationOptimizer();
    }
    return new Instantiator(this, component, optimizer);
  }

  /** An instantiator implementation that delegates to the module provided
   * factory if it defined one.
   */
  public static class Instantiator extends PojoInstantiator {

    /** The hibernate tuplizer, never null. */
    private transient HibernateComponentTuplizer tuplizer;

    /** The component to instantiate, never null. */
    private Component component;

    /** Caches the factory object that will instantiate the component.
     *
     * Initially null, initialized in getFactoryMethod if the component
     * is constructed by a factory.
     */
    private Object factoryObject = null;

    /** Caches the factory method that will instantiate the component.
     *
     * Initially null, initialized in getFactoryMethod if the component
     * is constructed by a factory.
     */
    private Method factoryMethod = null;

    /** Constructor, creates an instance of the instantiator.
     *
     * @param theTuplizer the hibernate tuplizer. This instantiator looks
     * in the registries provided by this tuplizer for the factory to create
     * new instances of the persistent class. This is never null.
     *
     * @param theComponent the component to instantiate, as passed to the
     * tuplizer by hibernate. It is never null.
     *
     * @param optimizer the instantiator optimizer, obtained from the
     * reflectionOptimizer. Null if none provided by the reflection optimizer.
     */
    public Instantiator(final HibernateComponentTuplizer theTuplizer,
        final Component theComponent,
        final InstantiationOptimizer optimizer) {
      super(theComponent, optimizer);
      Validate.notNull(theTuplizer, "The tuplizer cannot be null");
      tuplizer = theTuplizer;
      component = theComponent;
    }

    /** Creates an instance of the component.
     *
     * This implementation looks for the factory registered in one of the
     * HibernateRegistries and calls the parameterless create operation. If
     * the module did not defined a factory for that class, it delegates to
     * the default hibernate instantiator.
     *
     * {@inheritDoc}.*/
    @Override
    public Object instantiate() {
      Method create = getFactoryMethod();
      if (create != null) {
        return ReflectionUtils.invokeMethod(create, factoryObject);
      } else {
        return super.instantiate();
      }
    }

    /** Obtains the factory method.
     *
     * @return a method or null if this component is not created by a factory.
     */
    synchronized
    private Method getFactoryMethod() {
      if (factoryMethod == null) {
        for (HibernateRegistry registry : tuplizer.registries) {
          factoryObject = registry.getFactoryFor(
              component.getComponentClass());
          if (factoryObject != null) {
            factoryMethod = ReflectionUtils.findMethod(
                factoryObject.getClass(), "create");
            factoryMethod.setAccessible(true);
            break;
          }
        }
      }
      return factoryMethod;
    }
  }
}

