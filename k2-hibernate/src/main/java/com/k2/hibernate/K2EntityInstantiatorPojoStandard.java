package com.k2.hibernate;

import org.apache.commons.lang3.Validate;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.descriptor.java.JavaType;

/** A proxy class to a EntityInstantiatorPojoStandard.
 * It handles instantiation to a custom k2 entity instantiation. */
public class K2EntityInstantiatorPojoStandard
  implements EntityRepresentationStrategy {

  /** The proxied entity representation strategy. Never null. */
  private final EntityRepresentationStrategy entityRepresentationStrategy;

  /** The mapped type of the persistent class. Never null. */
  private final JavaType<?> mappedJavaType;

  /** The custom entity instantiator that handles actual instantiation.
   * Never null. */
  private final K2EntityInstantiator k2EntityInstantiator;

  /** Constructor.
   *
   * @param theEntityRepresentationStrategy the entity representation
   * strategy being proxied. Cannot be null.
   *
   * @param hibernateRegistry required by the actual entity instantiator to
   * provide the factory method for the persistent class. Cannot be null.
   *
   * @param persistentClass the class whose instantiation is being intercepted.
   * Cannot be null.
   */
  public K2EntityInstantiatorPojoStandard(
      final EntityRepresentationStrategy theEntityRepresentationStrategy,
      final HibernateRegistry hibernateRegistry,
      final Class<?> persistentClass) {
    Validate.notNull(theEntityRepresentationStrategy,
      "The EntityRepresentationStrategy cannot be null.");
    Validate.notNull(hibernateRegistry,
      "The HibernateRegistry cannot be null.");
    Validate.notNull(persistentClass,
      "The PersistentClass cannot be null.");

    entityRepresentationStrategy = theEntityRepresentationStrategy;
    mappedJavaType = entityRepresentationStrategy.getMappedJavaType();
    k2EntityInstantiator =
      new K2EntityInstantiator(persistentClass, hibernateRegistry);
  }

  @Override
  public EntityInstantiator getInstantiator() {
    return k2EntityInstantiator;
  }

  @Override
  public ProxyFactory getProxyFactory() {
    return entityRepresentationStrategy.getProxyFactory();
  }

  @Override
  public JavaType<?> getProxyJavaType() {
    JavaType<?> proxyJavaType = entityRepresentationStrategy.getProxyJavaType();
    return proxyJavaType;
  }

  @Override
  public RepresentationMode getMode() {
    return RepresentationMode.POJO;
  }

  @Override
  public ReflectionOptimizer getReflectionOptimizer() {
    return entityRepresentationStrategy.getReflectionOptimizer();
  }

  @Override
  public JavaType<?> getMappedJavaType() {
    return mappedJavaType;
  }

  @Override
  public PropertyAccess resolvePropertyAccess(final Property theProperty) {
    return entityRepresentationStrategy.resolvePropertyAccess(theProperty);
  }
}
