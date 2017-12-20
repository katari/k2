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
    application.run(new String[] {"--server.port=0"});
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

    assertThat(content, containsString("create table tm_entity_1"));
    assertThat(content, containsString("tm_uk_entity_3_unique_value"));
    assertThat(content, containsString("create index idx_entity_1_id"));

    // A ManyToOne joined by column.
    assertThat(content, containsString("tm_fk_entity_3_entity_1_id"));

    // An element collection with a long.
    assertThat(content, containsString("create table tm_entity_1_longs"));
    assertThat(content, containsString("tm_fk_entity_1_longs_entity_1_id"));

    // An element collection with an embeddable.
    assertThat(content, containsString("create table tm_entity_1_values"));
    assertThat(content, containsString("tm_fk_entity_1_values_entity_1_id"));

    // A many to many relation table.
    assertThat(content, containsString("create table tm_entity_1_entities"));
    assertThat(content, containsString("tm_fk_entity_1_entities_entities_id"));

    // A single table inheritance table name.
    assertThat(content, containsString("create table tm_base_class"));
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

      // These classes test the naming convention for single table inheritance.
      hibernateRegistry.registerPersistentClass(BaseClass.class);
      hibernateRegistry.registerPersistentClass(SubClass1.class);
      hibernateRegistry.registerPersistentClass(SubClass2.class);
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
  ///////////    The test application   ///////////////////////////////
  /////////////////////////////////////////////////////////////////////
  @Configuration
  public static class TestApplication extends Application {

    public TestApplication() {
      super(new Hibernate(), new Module1());
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

