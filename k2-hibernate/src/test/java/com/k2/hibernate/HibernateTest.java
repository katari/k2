/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import com.k2.core.Application;
import com.k2.core.Module;
import com.k2.core.ModuleContext;
import com.k2.core.Public;
import com.k2.core.Registrator;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;

public class HibernateTest {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(HibernateTest.class);

  private Application application;

  @Before public void setUp() {
    log.trace("Entering setUp");
    application = new TestApplication();
    application.run(new String[] {"--server.port=0",
        "--hibernate.k2.namingStrategy"
        + "=com.k2.hibernate.K2DbImplicitNamingStrategyComponentPath"});
    log.trace("Leaving setUp");
  }

  @After public void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void initialize() {
    assertThat(
        application.getBean(Hibernate.class, "sessionFactory", Object.class),
        is(not(nullValue())));
  }

  @Test public void dataSource_isPublic() {
    Object ds = application.getBean("hibernate.dataSource", Object.class);
    assertThat(ds, is(not(nullValue())));
  }

  @Test public void save() {
    EntityRepository repo = application.getBean(
        "testmodule.entity1Repository", EntityRepository.class);

    repo.save(new Entity1("first value"));
    repo.save(new Entity1("second value"));
    List<Entity1> result = repo.listEntity1();

    assertThat(result.size(), is(2));
    assertThat(result.get(0).getId(), is(1L));
    assertThat(result.get(0).getValue(), is("first value"));
    assertThat(result.get(1).getId(), is(2L));
    assertThat(result.get(1).getValue(), is("second value"));
  }

  @Test public void save_withFactory() {
    EntityRepository repo = application.getBean(
        "testmodule.entity1Repository", EntityRepository.class);

    Entity2Factory factory = application.getBean(Module1.class,
        "entity2Factory", Entity2Factory.class);

    repo.save(factory.create("first value"));
    repo.save(factory.create("second value"));
    List<Entity2> result = repo.listEntity2();

    assertThat(result.size(), is(2));
    assertThat(result.get(0).getParameter().toString(),
        is("Entity 2 factory parameter"));
    assertThat(result.get(0).getId(), is(1L));
    assertThat(result.get(0).getValue(), is("first value"));

    assertThat(result.get(1).getParameter().toString(),
        is("Entity 2 factory parameter"));
    assertThat(result.get(1).getId(), is(2L));
    assertThat(result.get(1).getValue(), is("second value"));
  }

  @Test public void save_withConverter() {
    EntityRepository repo = application.getBean(
        "testmodule.entity1Repository", EntityRepository.class);

    Entity2Factory factory = application.getBean(Module1.class,
        "entity2Factory", Entity2Factory.class);

    repo.save(factory.create("first value"));
    List<Entity2> result = repo.listEntity2();

    // Phone and Address are custom data types with a converter.
    assertThat(result.size(), is(1));
    assertThat(result.get(0).getParameter().toString(),
        is("Entity 2 factory parameter"));
    assertThat(result.get(0).getPhone(), is(not(nullValue())));
    assertThat(result.get(0).getPhone().getNumber(), is("555-5555"));
    assertThat(result.get(0).getAddress(), is(not(nullValue())));
    assertThat(result.get(0).getAddress().getStreet(), is("Corrientes"));
  }

  @Test public void save_withComponentFactory() {
    EntityRepository repo = application.getBean(
        "testmodule.entity1Repository", EntityRepository.class);

    Value1Factory factory1 = new Value1Factory();
    Value2Factory factory2 = new Value2Factory();

    Entity1 entity = new Entity1("an entity");
    entity.addValue2(factory2.create("one value"));
    entity.addValue2(factory2.create("another value"));
    entity.setValue1(factory1.create("a value1 instance"));
    repo.save(entity);
    List<Entity1> result = repo.listEntity1();

    assertThat(result.size(), is(1));
    assertThat(result.get(0).getValue2List().get(0).getInjected(),
        is("value 2 injected value"));
    assertThat(result.get(0).getValue1().getInjected(),
        is("value 1 injected value"));
  }

  @Test public void generateSchema() {
    SchemaGenerator schema = application.getBean(Hibernate.class, "schema",
        SchemaGenerator.class);
    schema.generate();

    String content = "";
    try (Scanner scanner = new Scanner(new File("target/schema.ddl"))) {
      content = scanner.useDelimiter("\\A").next();
    } catch (FileNotFoundException e) {
      throw new RuntimeException("target/schema.ddl not found", e);
    }

    // Check the correct prefix for table and unique index.
    assertThat(content, containsString("create table tm_entity_1"));
    assertThat(content, containsString("tm_uk_entity_3_unique_value"));

    assertThat(content, containsString("create index idx_entity_1_id"));

    // A ManyToOne joined by column.
    assertThat(content, containsString("tm_fk_entity_3_entity_1_id"));

    // An element collection with a long.
    assertThat(content, containsString("create table tm_entity_1_longs"));
    assertThat(content, containsString("tm_fk_entity_1_longs_entity_1_id"));

    // Check the correct prefix for an embedded column.
    assertThat(content, not(containsString("&&")));
    // The Entity1 contains an embedable named value1.
    assertThat(content, containsString("value_1_value varchar"));
    // An embedded with another embedded.
    assertThat(content, containsString("value_1_value_2_value varchar"));

    // An embedded without the attribute prefix, using @SkipComponentPrefix.
    assertThat(content, containsString("value varchar"));
    assertThat(content, containsString("value_2_value varchar"));

    // An element collection with an embeddable.
    assertThat(content, containsString(
          "create table tm_entity_1_value_2_list"));
    assertThat(content, containsString(
          "tm_fk_entity_1_value_2_list_entity_1_id"));
    assertThat(content, containsString("value_2_list_value varchar"));

    // A many to many relation table and fk.
    assertThat(content, containsString(
        "create table tm_entity_1_many_entities"));
    assertThat(content, containsString(
        "tm_fk_entity_1_many_entities_many_entities_id"));
    assertThat(content, containsString(
        "tm_fk_entity_1_many_entities_entity_1_id"));

    // A one to many fk, specified with @ForeignKey.
    assertThat(content, containsString("tm_fk_entity_1_one"));

    // A single table per class hierarchy table name.
    assertThat(content, containsString(
          "create table tm_single_table_base_class"));

    // All mapped super class hierarchy table names.
    assertThat(content, containsString(
          "create table tm_mapped_super_sub_class_1"));
    assertThat(content, containsString(
          "create table tm2_mapped_super_sub_class_2"));

    // All joined inheritance table names.
    assertThat(content, containsString("create table tm_joined_base_class"));
    assertThat(content, containsString("create table tm_joined_sub_class_1"));
    assertThat(content, containsString("create table tm2_joined_sub_class_2"));

    // All table per class table names.
    assertThat(content, containsString(
          "create table tm_table_per_class_base_class"));
    assertThat(content, containsString(
          "create table tm_table_per_class_sub_class_1"));
    assertThat(content, containsString(
          "create table tm2_table_per_class_sub_class_2"));

    // Just in case, the tm_tm_ prefix should never appear.
    assertThat(content, not(containsString("tm_tm_")));
  }

  // Sample class to create beans in the test application.
  public static class StringHolder {
    private String value;

    public StringHolder(final String theValue) {
      value = theValue;
    }

    public String toString() {
      return value;
    }
  }

  /////////////////////////////////////////////////////////////////////
  ///////////    The module 1 declaration   ///////////////////////////
  /////////////////////////////////////////////////////////////////////
  @EnableTransactionManagement(proxyTargetClass = true)
  @Configuration("testmodule")
  @Module(shortName = "tm")
  public static class Module1 implements Registrator {

    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      HibernateRegistry hibernateRegistry;
      hibernateRegistry = moduleContext.get(HibernateRegistry.class);
      hibernateRegistry.registerPersistentClass(Entity1.class);
      hibernateRegistry.registerPersistentClass(Entity2.class,
          Entity2Factory.class);
      hibernateRegistry.registerPersistentClass(Entity3.class);
      hibernateRegistry.registerPersistentClass(Entity4.class);
      hibernateRegistry.registerPersistentClass(Value1.class,
          Value1Factory.class);
      hibernateRegistry.registerPersistentClass(Value2.class,
          Value2Factory.class);

      // These classes test the naming convention for single table per class
      // hierarchy.
      hibernateRegistry.registerPersistentClass(SingleTableBaseClass.class);
      hibernateRegistry.registerPersistentClass(SingleTableSubClass1.class);

      // These classes test the naming convention for mapped superclass
      // inheritance.
      hibernateRegistry.registerPersistentClass(MappedSuperBaseClass.class);
      hibernateRegistry.registerPersistentClass(MappedSuperSubClass1.class);

      // These classes test the naming convention for joined superclass table
      // inheritance.
      hibernateRegistry.registerPersistentClass(JoinedBaseClass.class);
      hibernateRegistry.registerPersistentClass(JoinedSubClass1.class);

      // These classes test the naming convention for one table per class
      // inheritance.
      hibernateRegistry.registerPersistentClass(TablePerClassBaseClass.class);
      hibernateRegistry.registerPersistentClass(TablePerClassSubClass1.class);

      hibernateRegistry.registerConverter(Phone.Converter.class);
      hibernateRegistry.registerConverter(Address.Converter.class);
    }

    @Bean @Public public EntityRepository entity1Repository(
        final SessionFactory sessionFactory) {
      return new EntityRepository(sessionFactory);
    }

    @Bean(name = "parameter") public StringHolder parameter() {
      return new StringHolder("Entity 2 factory parameter");
    }

    @Bean public Entity2Factory entity2Factory(
        @Qualifier("parameter") final StringHolder parameter) {
      return new Entity2Factory(parameter);
    }

    @Bean public Value1Factory value1Factory() {
      return new Value1Factory();
    }

    @Bean public Value2Factory value2Factory() {
      return new Value2Factory();
    }
  };

  /////////////////////////////////////////////////////////////////////
  ///////////    The entity 2 factory /////////////////////////////////
  /////////////////////////////////////////////////////////////////////
  public static class Entity2Factory {
    private StringHolder parameter;

    Entity2Factory(final StringHolder param) {
      parameter = param;
    }

    Entity2 create() {
      return new Entity2(parameter);
    }
    Entity2 create(final String value) {
      return new Entity2(parameter, value);
    }
  }

  /////////////////////////////////////////////////////////////////////
  ///////////    The value factories /////////////////////////////////
  /////////////////////////////////////////////////////////////////////
  public static class Value1Factory {
    private String injected = "value 1 injected value";

    Value1 create() {
      return new Value1(injected);
    }

    Value1 create(final String value) {
      return new Value1(injected, value);
    }
  }

  public static class Value2Factory {
    private String injected = "value 2 injected value";

    Value2 create() {
      return new Value2(injected);
    }

    Value2 create(final String value) {
      return new Value2(injected, value);
    }
  }

  /////////////////////////////////////////////////////////////////////
  ///////////    The repository    ////////////////////////////////////
  /////////////////////////////////////////////////////////////////////
  @Transactional
  public static class EntityRepository {
    private SessionFactory sessionFactory;

    EntityRepository(final SessionFactory theSessionFactory) {
      sessionFactory = theSessionFactory;
    }

    public void save(final Entity1 instance) {
      Session session = sessionFactory.getCurrentSession();
      session.save(instance);
    }

    public void save(final Entity2 instance) {
      Session session = sessionFactory.getCurrentSession();
      session.save(instance);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public List<Entity1> listEntity1() {
      Session session = sessionFactory.getCurrentSession();
      return session.createCriteria(Entity1.class)
          .addOrder(Order.asc("id"))
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
          .list();
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public List<Entity2> listEntity2() {
      Session session = sessionFactory.getCurrentSession();
      return session.createCriteria("com.k2.hibernate.Entity2")
          .addOrder(Order.asc("id"))
          .list();
    }
  };

  /////////////////////////////////////////////////////////////////////
  ///////////    The module 2 declaration   ///////////////////////////
  /////////////////////////////////////////////////////////////////////
  @EnableTransactionManagement(proxyTargetClass = true)
  @Configuration("testmodule2")
  @Module(shortName = "tm2")

  public static class Module2 implements Registrator {
    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      HibernateRegistry hibernateRegistry;
      hibernateRegistry = moduleContext.get(HibernateRegistry.class);

      // These classes test the naming convention when different classe in the
      // same hierachy belong to different modules.
      hibernateRegistry.registerPersistentClass(SingleTableSubClass2.class);
      hibernateRegistry.registerPersistentClass(MappedSuperSubClass2.class);
      hibernateRegistry.registerPersistentClass(JoinedSubClass2.class);
      hibernateRegistry.registerPersistentClass(TablePerClassSubClass2.class);
    }
  }

  /////////////////////////////////////////////////////////////////////
  ///////////    The test application   ///////////////////////////////
  /////////////////////////////////////////////////////////////////////
  @Configuration
  public static class TestApplication extends Application {

    public TestApplication() {
      super(new Hibernate(), new Module1(), new Module2());
      setWebEnvironment(false);
    }

    @Bean public List<Class<?>> persistentClasses() {
      List<Class<?>> classes = new LinkedList<Class<?>>();
      classes.add(Entity1.class);
      return classes;
    }

    @Bean public Class<?> persistentClass() {
      return Entity1.class;
    }
  }
}

