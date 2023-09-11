package com.k2.hibernate;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

/** A proxy class to an hibernate EmbeddableRepresentationStrategy.
 * It handles instantiation to a custom k2 component instantiation. */
public class K2EmbeddableInstantiatorPojoStandard
  implements EmbeddableRepresentationStrategy {

  /** The proxied embeddable representation strategy. Never null. */
  private final EmbeddableRepresentationStrategy defaultStrategy;

  /** The custom component instantiator that handles actual instantiation.
   * Never null. */
  private final K2ComponentInstantiator k2ComponentInstantiator;

  /** Constructor.
   *
   * @param theDefaultStrategy the embeddable representation strategy being
   * proxied. Cannot be null.
   *
   * @param hibernateRegistry required by the actual component instantiator to
   * provide the factory method for the persistent class. Cannot be null.
   *
   * @param theComponentClass the class whose instantiation is being
   * intercepted. Cannot be null.
   *
   */
  public K2EmbeddableInstantiatorPojoStandard(
      final EmbeddableRepresentationStrategy theDefaultStrategy,
      final HibernateRegistry hibernateRegistry,
      final Class<?> theComponentClass) {
    defaultStrategy = theDefaultStrategy;
    k2ComponentInstantiator =
      new K2ComponentInstantiator(theComponentClass, hibernateRegistry);
  }

  @Override
  public EmbeddableInstantiator getInstantiator() {
    return k2ComponentInstantiator;
  }

  @Override
  public RepresentationMode getMode() {
    return defaultStrategy.getMode();
  }

  @Override
  public ReflectionOptimizer getReflectionOptimizer() {
    return defaultStrategy.getReflectionOptimizer();
  }

  @Override
  public JavaType<?> getMappedJavaType() {
    return defaultStrategy.getMappedJavaType();
  }

  @Override
  public PropertyAccess resolvePropertyAccess(final Property theProperty) {
    return defaultStrategy.resolvePropertyAccess(theProperty);
  }
}
