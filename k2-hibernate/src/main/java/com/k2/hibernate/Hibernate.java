/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.stereotype.Component;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Table.ForeignKeyKey;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.service.Service;

import com.k2.core.K2Environment;
import com.k2.core.RegistryFactory;
import com.k2.core.ModuleDefinition;
import com.k2.core.Public;

/** The hibernate module.
 *
 * To use this module, create a class that extends Application and in the
 * constructor call:
 *
 * super(new Hibernate(), ....);
 *
 * If you want to write a module that exposes persistent classes, make your
 * module implement Registrator and in the addRegistration operation call:
 *
 * moduleContext.get(HibernateRegistry.class)
 *     .registerPersistentClass(Entity1.class);
 *
 * to let Hibernate manage your persistent classes. See HibernateRegistry for
 * more information.
 *
 * This module will read all the properties that start with 'hibernate.' and
 * use them to configure the session factory.
 *
 * It also implements a custom hibernate naming strategy: all lower case
 * symbols and underscore separated words. It attempts to create indexes and
 * primary keys with a recognizable name. The downside is that it generates
 * pretty long names, so not all databases support it. You can fall back to
 * hibernate default by setting the hibernate.k2.useK2Naming property to false.
 *
 * The module reads the following properties:
 *
 * hibernate.k2.usePrefix: if true, adds the module short name to each table
 * and foreign key name. Defaults to true.
 *
 * hibernate.k2.useK2Naming: if true, uses the k2 database naming convention:
 * all lower case, underscore separated.
 */
@Component("hibernate")
@PropertySource("classpath:/com/k2/hibernate/hibernate.properties")
public class Hibernate implements RegistryFactory {

  /** The registries requested by all modules, never null.
   */
  private List<HibernateRegistry> registries = new LinkedList<>();

  /** Creates a hibernate registry for the provided module and stores it in the
   * registries.
   *
   * The registries list is available to the HibernateTuplizer through the
   * HibernateRegistryLocator service.
   */
  @Override
  public HibernateRegistry getRegistry(final ModuleDefinition requestor) {
    HibernateRegistry result = new HibernateRegistry(requestor);
    registries.add(result);
    return result;
  }

  /** Defines the transaction manager to use.
   *
   * @param sessionFactory the session factory. It cannot be null.
   *
   * @return the Hibernate Transaction manager, never returns null.
   */
  @Public @Bean(name = "transactionManager")
  public HibernateTransactionManager transactionManager(
      final SessionFactory sessionFactory) {
    HibernateTransactionManager txManager = new HibernateTransactionManager();
    txManager.setSessionFactory(sessionFactory);
    return txManager;
  }

  /** Hibernate metadata.
   *
   * The hibernate metadata is initialized from the configuration, module
   * provided entities and factories. Application writers can use this metadata
   * to generate the ddl.
   *
   * @param environment the environment provided by k2 core, used by hibernate
   * to obtain its properties.
   *
   * @param useK2Naming true to use the k2 database naming conventions. False
   * uses hibernate default.
   *
   * @param usePrefix true to add the module short name as a prefix to each
   * database object. Defaults to true.
   *
   * @param dataSource the data source, never null.
   *
   * @return the Hibernate's metadata, never returns null.
   */
  @Bean public Metadata metadata(
      final K2Environment environment,
      @Value("${hibernate.k2.useK2Naming:#{true}}") final boolean useK2Naming,
      @Value("${hibernate.k2.usePrefix:#{true}}") final boolean usePrefix,
      final DataSource dataSource) {
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
      .applySetting("hibernate.connection.datasource", dataSource)
      .applySetting("hibernate.current_session_context_class",
          "org.springframework.orm.hibernate5.SpringSessionContext")
      .applySettings(environment.getProperties("hibernate"))
      .addService(this.getClass(), new HibernateRegistryLocator(registries))
      .build();

    Map<Class<?>, String> prefixes = new HashMap<>();

    MetadataSources metadataSources = new MetadataSources(registry);
    for (HibernateRegistry hibernateRegistry: registries) {
      for (Class<?> entity : hibernateRegistry.getPersistentClasses()) {
        metadataSources.addAnnotatedClass(entity);
        prefixes.put(entity, hibernateRegistry.getRequestorPrefix());
      }
    }

    MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder()
        .enableNewIdentifierGeneratorSupport(false);
    if (useK2Naming) {
      metadataBuilder.applyImplicitNamingStrategy(
          new K2DbImplicitNamingStrategy());
    }
    Metadata metadata = metadataBuilder.build();

    // Rename collection tables and their related elements.
    for (Collection c : metadata.getCollectionBindings()) {
      Table table = c.getCollectionTable();
      if (usePrefix) {
        String prefix = prefixes.get(c.getOwner().getMappedClass());
        prefixDddlElements(prefix, table);
      }
    }

    // Renames entity tables, their related elements and configures the
    // tuplizers.
    for (PersistentClass pc : metadata.getEntityBindings()) {
      Table table = pc.getTable();

      if (usePrefix) {
        String prefix = prefixes.get(pc.getMappedClass());
        prefixDddlElements(prefix, table);
      }

      pc.addTuplizer(EntityMode.POJO, HibernateTuplizer.class.getName());

      MetaAttribute attribute = new MetaAttribute("k2.moduleContext");
      Map<String, MetaAttribute> attributes = new HashMap<>();
      attributes.put("k2.moduleContext",  attribute);
      pc.setMetaAttributes(attributes);
    }

    return metadata;
  }

  /** Renames the table and its related elements based on the module prefix.
   *
   * @param prefix the prefix to add to ddl element names. It cannot be null.
   *
   * @param table the table object that contains the elements to rename. It
   * cannot be null.
   */
  private void prefixDddlElements(final String prefix, final Table table) {
    // Add the module prefix to the table name.
    table.setName(prefix + "_" + table.getName());

    // Add the module prefix to each foreign key name.
    for (Map.Entry<ForeignKeyKey, ForeignKey> entry
        : table.getForeignKeys().entrySet()) {
      ForeignKey foreignKey = entry.getValue();
      foreignKey.setName(prefix + "_" + foreignKey.getName());
    }

    // Add the module prefix to each unique key name.
    Iterator<UniqueKey> uniqueKeys = table.getUniqueKeyIterator();
    while (uniqueKeys.hasNext()) {
      UniqueKey uniqueKey = uniqueKeys.next();
      uniqueKey.setName(prefix + "_" + uniqueKey.getName());
    }
  }

  /** Hibernate SessionFactory.
   *
   * @param metadata the hibernate metadata, initialized with hibernate
   * configuration and module provided entities and factories. I cannot be
   * null.
   *
   * @return the Hibernate's SessionFactory, never returns null.
   */
  @Public @Bean public SessionFactory sessionFactory(final Metadata metadata) {
    return metadata.getSessionFactoryBuilder().build();
  }

  /** The tomcat jdbc pool properties.
   *
   * This is initialized from properties that start with 'datasource'.
   *
   * @return the pool properties, never null.
   */
  @ConfigurationProperties(prefix = "datasource")
  @Bean public PoolProperties poolProperties() {
    PoolProperties properties = new PoolProperties();
    return properties;
  }

  /** Creates a data source used by hibernate.
   *
   * @param poolProperties the properties to configure the datasource and pool.
   * It cannot be null.
   *
   * @return the data source, never null.
   */
  @Bean public DataSource dataSource(final PoolProperties poolProperties) {
    return new DataSource(poolProperties);
  }

  /** Bean to generate the schema based on hibernate configuration.
   *
   * @param metadata the properly initialized hibernate metadata. It cannot be
   * null.
   *
   * @return an instance of SchemaGenerator, never null.
   */
  @Bean SchemaGenerator schema(final Metadata metadata) {
    return new SchemaGenerator(metadata);
  }

  /** A hibernate service that exposes the hibernate module registries to the
   * hibernate tuplizers.
   */
  @SuppressWarnings("serial")
  static class HibernateRegistryLocator implements Service {

    /** The registries to expose to the hibernate tuplizers, never null. */
    private transient List<HibernateRegistry> registries;

    /** Constructor, creates a hibernate registry locator.
     *
     * @param theRegistries the registries to expose. It cannot be null.
     */
    HibernateRegistryLocator(final List<HibernateRegistry> theRegistries) {
      Validate.notNull(theRegistries, "The registries cannot be null.");
      registries = theRegistries;
    }

    /** Returns the registries, used by HibernateTuplizer to instantiate the
     * module persistent classes.
     *
     * @return the list of registries, never returns null.
     */
    List<HibernateRegistry> getRegistries() {
      return registries;
    }
  }
}

