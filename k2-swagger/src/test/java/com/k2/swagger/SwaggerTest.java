/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.AfterClass;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.k2.core.Application;
import com.k2.core.ModuleContext;
import com.k2.core.Registrator;
import com.k2.core.K2Environment;

import com.k2.swagger.api.InventoryApiController;
import com.k2.swagger.api.InventoryApiTest;

public class SwaggerTest {

  /** The class logger. */
  private static Logger log = LoggerFactory.getLogger(SwaggerTest.class);

  private static Application application;

  private static String baseUrl = "http://localhost:";

  private static Executor executor;

  @BeforeClass public static void setUp() {
    log.trace("Entering setUp");

    application = new TestApplication();
    application.run(new String[] {"--server.port=0"});

    K2Environment environment;
    environment = (K2Environment) application.getBean("environment");
    String port = environment.getProperty("local.server.port");
    baseUrl = baseUrl + port;

    CloseableHttpClient httpClient = HttpClientBuilder.create()
      .setRedirectStrategy(new LaxRedirectStrategy()).build();
    executor = Executor.newInstance(httpClient);

    log.trace("Leaving setUp");
  }

  @AfterClass public static void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void specList() throws Exception {
    String endpoint = baseUrl + "/swagger/";

    String page;
    page = executor.execute(Request.Get(endpoint)).returnContent().asString();
    // Checks that the generated page contains the swagger spec from both
    // modules.
    assertThat(page, containsString("api1.yaml"));
    assertThat(page, containsString("api2.yaml"));
  }

  /////////////////////////////////////////////////////////////////////
  ///////////    The test application   ///////////////////////////////
  /////////////////////////////////////////////////////////////////////
  @Configuration
  public static class TestApplication extends Application {

    public TestApplication() {
      super(new Swagger(), new Module1(), new Module2());
    }

    public static void main(final String ... args) {
      Application testApplication = new TestApplication();
      testApplication.run(args);
    }
  }

  @Component("module1")
  public static class Module1 implements Registrator {
    @Bean public InventoryApiController controller() {
      return new InventoryApiTest().createController();
    }
    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      SwaggerRegistry registry = moduleContext.get(SwaggerRegistry.class);
      registry.registerIdl("/module1/static/api1.yaml");
    }
  }

  @Component("module2")
  public static class Module2 implements Registrator {
    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      SwaggerRegistry registry = moduleContext.get(SwaggerRegistry.class);
      registry.registerIdl("/module2/static/api2.yaml");
    }
  }
}

