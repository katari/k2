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
 * The tuplizer logic in general has things to improve:
 *
 * 1- Cache the factory Method instance, to speed up the call to create().
 *
 * 2- Implement a tuplizer for pojo components (PojoComponentTuplizer).
 */
public class HibernateTuplizer extends PojoEntityTuplizer {

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
  public HibernateTuplizer(final EntityMetamodel entityMetamodel,
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
    private HibernateTuplizer tuplizer;

    /** The persistent class to instantiate, never null. */
    private PersistentClass persistentClass;

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
    public Instantiator(final HibernateTuplizer theTuplizer,
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
      Object factory = null;
      for (HibernateRegistry registry : tuplizer.registries) {
        factory = registry.getFactoryFor(persistentClass.getMappedClass());
        if (factory != null) {
          Method create = ReflectionUtils.findMethod(
              factory.getClass(), "create");
          create.setAccessible(true);
          return ReflectionUtils.invokeMethod(create, factory);
        }
      }
      return super.instantiate();
    }
  }
}

