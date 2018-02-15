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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json
    .MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.k2.core.Application;
import com.k2.core.ModuleContext;
import com.k2.core.Registrator;
import com.k2.core.K2Environment;
import com.k2.core.Module;

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
    environment = application.getBean("environment", K2Environment.class);
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
    assertList(baseUrl + "/swagger");
  }

  @Test public void specList_endsInSlash() throws Exception {
    assertList(baseUrl + "/swagger/");
  }

  /** Checks that the generated page contains the swagger specs from both
   * modules.
   *
   * @param endpoint the swagger endpoint. It cannot be null.
   */
  private void assertList(final String endpoint) throws Exception {
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
  @Module(relativePath = "../k2-swagger/src/test/resources")
  public static class Module1 implements Registrator {
    @Bean public InventoryApiController controller() {
      return new InventoryApiTest().createController();
    }
    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      SwaggerRegistry registry = moduleContext.get(SwaggerRegistry.class);
      registry.registerIdl("/module1/static/api1.yaml");
    }

    @Bean public MappingJackson2HttpMessageConverter
        mappingJackson2HttpMessageConverter() {
      ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
      mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      return new MappingJackson2HttpMessageConverter(mapper);
    }
  }

  @Component("module2")
  @Module(relativePath = "../k2-swagger/src/test/resources")
  public static class Module2 implements Registrator {
    @Bean public InventoryApiController controller() {
      return new InventoryApiTest().createController();
    }
    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      SwaggerRegistry registry = moduleContext.get(SwaggerRegistry.class);
      registry.registerIdl("/module2/static/api2.yaml");
    }
  }
}

