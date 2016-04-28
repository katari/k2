/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import com.k2.core.Application;
import com.k2.core.ModuleContext;
import com.k2.core.Public;
import com.k2.core.Registrator;

public class HibernateTest {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(HibernateTest.class);

  private Application application;

  @Before public void setUp() {
    log.trace("Entering setUp");
    application = new TestApplication();
    application.run(new String[0]);
    log.trace("Leaving setUp");
  }

  @After public void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void initialize() {
    assertThat(application.getBean(Hibernate.class, "sessionFactory"),
        is(not(nullValue())));
  }

  @Test public void save() {
    EntityRepository repo = (EntityRepository) application.getBean(
        "testmodule.entity1Repository");

    repo.save(new Entity1("first value"));
    repo.save(new Entity1("second value"));
    List<Entity1> result = repo.listEntity1();

    assertThat(result.size(), is(2));
    assertThat(result.get(0).id, is(1L));
    assertThat(result.get(0).value, is("first value"));
    assertThat(result.get(1).id, is(2L));
    assertThat(result.get(1).value, is("second value"));
  }

  @Test public void save_withFactory() {
    EntityRepository repo = (EntityRepository) application.getBean(
        "testmodule.entity1Repository");

    Entity2Factory factory = (Entity2Factory) application.getBean(
        Module1.class, "entity2Factory");

    repo.save(factory.create("first value"));
    repo.save(factory.create("second value"));
    List<Entity2> result = repo.listEntity2();

    assertThat(result.size(), is(2));
    assertThat(result.get(0).parameter.toString(),
        is("Entity 2 factory parameter"));
    assertThat(result.get(0).id, is(1L));
    assertThat(result.get(0).value, is("first value"));

    assertThat(result.get(1).parameter.toString(),
        is("Entity 2 factory parameter"));
    assertThat(result.get(1).id, is(2L));
    assertThat(result.get(1).value, is("second value"));
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
  public static class Module1 implements Registrator {

    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      HibernateRegistry hibernateRegistry;
      hibernateRegistry = moduleContext.get(HibernateRegistry.class);
      hibernateRegistry.registerPersistentClass(Entity1.class);
      hibernateRegistry.registerPersistentClass(Entity2.class,
          Entity2Factory.class);
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
  ///////////    The entities /////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////
  @Entity
  @Table(name = "entity_1")
  public static class Entity1 {

    @Id @GeneratedValue private long id;
    private String value;

    Entity1() {
    }
    Entity1(final String theValue) {
      value = theValue;
    }
  };

  @Entity
  @Table(name = "entity_2")
  public static class Entity2 {

    @Transient
    private StringHolder parameter;

    @Id @GeneratedValue private long id;
    private String value;

    Entity2(final StringHolder param) {
      parameter = param;
    }

    Entity2(final StringHolder param, final String theValue) {
      parameter = param;
      value = theValue;
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

    @SuppressWarnings("unchecked")
    public List<Entity1> listEntity1() {
      Session session = sessionFactory.getCurrentSession();
      return session.createCriteria(Entity1.class)
          .addOrder(Order.asc("id"))
          .list();
    }

    @SuppressWarnings("unchecked")
    public List<Entity2> listEntity2() {
      Session session = sessionFactory.getCurrentSession();
      return session.createCriteria(Entity2.class)
          .addOrder(Order.asc("id"))
          .list();
    }
  };

  /////////////////////////////////////////////////////////////////////
  ///////////    The test application   ///////////////////////////////
  /////////////////////////////////////////////////////////////////////
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

