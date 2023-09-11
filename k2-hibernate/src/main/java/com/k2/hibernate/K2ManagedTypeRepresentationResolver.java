package com.k2.hibernate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;

/** Resolves the Representation Strategies for Entities and Components.
 * It chooses a custom Representation Strategy when dealing with entities or
 * components that need a factory instead of a default constructor on during
 * object instantiation.
 */
public class K2ManagedTypeRepresentationResolver
  implements ManagedTypeRepresentationResolver {

  /** Hibernate's default ManagedTypeRepresentationResolver. Never null. */
  private ManagedTypeRepresentationResolver hibernateTypeResolver;

  /** The RepresentationStrategies for each persistent class. Never null. */
  private Map<Class<?>, EntityRepresentationStrategy> strategyByEntity;

  /** The representationStrategies for each embeddable. Never null. */
  private Map<Class<?>, EmbeddableRepresentationStrategy> strategyByComponent;

  /** The Registry where each persistent class was registered. Never null. */
  private Map<Class<?>, HibernateRegistry> registryBySmartEntity;

  // TODO sp 2023-08-25 Esto tendría que recibir todos los beans de factories de
  //  un saque, lamentablemente para eso tienen que tener una interfaz.

  /** Constructor.
   *
   * @param defaultRepresentationResolver Hibernates default representation
   * resolver. Cannot be null.
   */
  public K2ManagedTypeRepresentationResolver(
      final ManagedTypeRepresentationResolver defaultRepresentationResolver) {
    Validate.notNull(defaultRepresentationResolver,
      "The ManagedTypeRepresentationResolver cannot be null.");
    hibernateTypeResolver = defaultRepresentationResolver;
    strategyByEntity = new HashMap<>();
    strategyByComponent = new HashMap<>();
    registryBySmartEntity = new HashMap<>();
  }

  /** Registers the Hibernate registry of a k2 module.
   *
   * @param registry to be tracked. Cannot be null.
   */
  void registerHibernateRegistry(final HibernateRegistry registry) {
    List<Class<?>> persistentClasses = registry.getPersistentClasses();
    for (Class<?> persistentClass : persistentClasses) {
      if (registry.hasFactoryFor(persistentClass)) {
        registryBySmartEntity.put(persistentClass, registry);
      }
    }
  }

  @Override
  public EntityRepresentationStrategy resolveStrategy(
      final PersistentClass persistentClass,
      final EntityPersister entityPersister,
      final RuntimeModelCreationContext runtimeModelCreationContext) {

    Class<?> entityClass = persistentClass.getMappedClass();
    EntityRepresentationStrategy strategy = strategyByEntity.get(entityClass);
    if (strategy != null) {
      return strategy;
    }

    EntityRepresentationStrategy defaultStrategy =
      hibernateTypeResolver.resolveStrategy(
        persistentClass, entityPersister, runtimeModelCreationContext);

    HibernateRegistry registry = registryBySmartEntity.get(entityClass);
    if (registry != null) {
      EntityRepresentationStrategy myCustomStrategy;
      myCustomStrategy = new K2EntityInstantiatorPojoStandard(
          defaultStrategy, registry, entityClass);
      strategyByEntity.put(entityClass, myCustomStrategy);
      return myCustomStrategy;
    }

    return defaultStrategy;
  }

  @Override
  public EmbeddableRepresentationStrategy resolveStrategy(
      final Component component, final Supplier<EmbeddableMappingType> supplier,
      final RuntimeModelCreationContext runtimeModelCreationContext) {

    EmbeddableRepresentationStrategy defaultStrategy
      = hibernateTypeResolver.resolveStrategy(component, supplier,
        runtimeModelCreationContext);

    Class<?> componentClass = component.getComponentClass();
    EmbeddableRepresentationStrategy strategy =
      strategyByComponent.get(componentClass);
    if (strategy != null) {
      return strategy;
    }

    HibernateRegistry registry = registryBySmartEntity.get(componentClass);
    if (registry != null) {
      EmbeddableRepresentationStrategy myCustomStrategy;
      myCustomStrategy =
        new K2EmbeddableInstantiatorPojoStandard(defaultStrategy,
          registry, componentClass);
      strategyByComponent.put(componentClass, myCustomStrategy);
      return myCustomStrategy;
    }

    return defaultStrategy;
  }
}
