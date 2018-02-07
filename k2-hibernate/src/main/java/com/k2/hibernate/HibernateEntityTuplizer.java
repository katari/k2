/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.List;

import java.lang.reflect.Method;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.util.ReflectionUtils;

import org.apache.commons.lang3.Validate;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityInstantiator;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/** K2 hibernate tuplizer that uses module provided factories to create
 * entity instances.
 *
 * See HibernateRegistry for more information.
 *
 * Note: this class is almost identical to HibernateComponentTuplizer. Look at
 * that class if you modify anything here.
 */
public class HibernateEntityTuplizer extends PojoEntityTuplizer {

  /** The list of registries with the entity classes and factories provided
   * by other modules, never null.
   */
  private List<HibernateRegistry> registries;

  /** A copy of the reflection optimizer obtained from the parent class, via
   * reflection, null if it is null in the parent class.
   */
  private ReflectionOptimizer reflectionOptimizer;

  /** Constructor, creates a hibernate tuplizer.
   *
   * @param entityMetamodel the entity metamodel as passed by hibernate, never
   * null.
   *
   * @param mappedEntity the persistent class to instantian, as passed by
   * hibernate. This is never null.
   */
  public HibernateEntityTuplizer(final EntityMetamodel entityMetamodel,
      final PersistentClass mappedEntity) {
    super(entityMetamodel, mappedEntity);

    registries = entityMetamodel.getSessionFactory().getServiceRegistry()
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
  protected Instantiator buildInstantiator(final EntityMetamodel metamodel,
      final PersistentClass persistentClass) {
    InstantiationOptimizer optimizer = null;
    if (reflectionOptimizer != null) {
      optimizer = reflectionOptimizer.getInstantiationOptimizer();
    }
    return new Instantiator(this, metamodel, persistentClass, optimizer);
  }

  /** An instantiator implementation that delegates to the module provided
   * factory if it defined one.
   */
  @SuppressWarnings("serial")
  public static class Instantiator extends PojoEntityInstantiator {

    /** The hibernate tuplizer, never null. */
    private transient HibernateEntityTuplizer tuplizer;

    /** The persistent class to instantiate, never null. */
    private PersistentClass persistentClass;

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
     * @param entityMetamodel the entity metamodel, as pass to the tuplizer by
     * hibernate. It is never null.
     *
     * @param thePersistentClass the class to instantiate, as pass to the
     * tuplizer by hibernate. It is never null.
     *
     * @param optimizer the instantiator optimizer, obtained from the
     * reflectionOptimizer. Null if none provided by the reflection optimizer.
     */
    public Instantiator(final HibernateEntityTuplizer theTuplizer,
        final EntityMetamodel entityMetamodel,
        final PersistentClass thePersistentClass,
        final InstantiationOptimizer optimizer) {
      super(entityMetamodel, thePersistentClass, optimizer);
      Validate.notNull(theTuplizer, "The tuplizer cannot be null");
      tuplizer = theTuplizer;
      persistentClass = thePersistentClass;
    }

    /** Creates an instance of the persistent class.
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
              persistentClass.getMappedClass());
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

