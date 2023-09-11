/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.LinkedList;
import java.util.List;
import java.time.OffsetDateTime;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.AfterClass;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.containsString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class ApplicationTest {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(ApplicationTest.class);

  private static boolean initCalled = false;

  private static Application application;

  private static String baseUrl = "http://localhost:";

  private static Executor executor;

  @BeforeClass public static void setUp() {
    initCalled = false;
    application = new WebApplication();
    application.run(new String[] {"--server.port=0",
      "--logging.file=target/log/test-overriden.log"});

    K2Environment environment;
    environment = application.getBean("environment", K2Environment.class);
    String port = environment.getProperty("local.server.port");
    baseUrl = baseUrl + port;

    CloseableHttpClient httpClient = HttpClientBuilder.create()
      .setRedirectStrategy(new LaxRedirectStrategy()).build();
    executor = Executor.newInstance(httpClient);
  }

  @AfterClass public static void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void emptyApplication() {
    Application emptyApplication = new EmptyApplication();
    emptyApplication.run(new String[] {"--server.port=0"});
    assertThat(emptyApplication.getApplication(), is(not(nullValue())));
    emptyApplication.stop();
  }

  @Test public void emptyApplicationConfigOption() {
    Application emptyApplication = new EmptyApplication();
    emptyApplication.run(new String[] {"--server.port=0"});
    assertThat(emptyApplication.getBean("option", String.class),
        is("configOption"));
    emptyApplication.stop();
  }

  @Test public void standAloneApplication() {
    Application standAloneApplication = new StandAloneApplication();
    standAloneApplication.run(new String[0]);
    assertThat(standAloneApplication.getApplication(), is(not(nullValue())));
    assertThat(standAloneApplication.getBean(Module1.class, "testBean",
        Object.class).toString(), is("Module 1 private bean"));
    standAloneApplication.stop();
  }

  @Test public void standAloneConfigOption() {
    Application standAloneApplication;
    standAloneApplication = new StandAloneApplication();
    standAloneApplication.run(new String[0]);
    assertThat(standAloneApplication.getBean("option", String.class),
        is("configOption"));
    standAloneApplication.stop();
  }

  @Test public void getApplication_beforeRun() {
    Application nonRunningApp = new WebApplication();
    assertThat(nonRunningApp.getApplication(), is(not(nullValue())));
    nonRunningApp.stop();
  }

  @Test public void getApplication_afterRun() {
    log.trace("Entering getApplication_afterRun");
    assertThat(application.getApplication(), is(not(nullValue())));
    log.trace("Leaving getApplication_afterRun");
  }

  @Test public void getBean_privateBean() {
    assertThat(application.getBean(Module1.class, "testBean", Object.class)
        .toString(), is("Module 1 private bean"));
  }

  @Test public void getBean_globalBean() {
    assertThat(application.getBean("globalBean", String.class),
        is("Global bean"));
  }

  @Test public void getBean_exposedBean() {
    assertThat(application.getBean("testmodule.exposedBean",
        StringHolder.class).toString(), is("Module 1 exposed bean"));
  }

  @Test public void getBean_renamedExposedBean() {
    assertThat(application.getBean("testmodule.renamedExposedBean",
          StringHolder.class).toString(), is("Module 1 renamed exposed bean"));
  }

  @Test public void getBean_injectedWithExposedBean() {
    assertThat(
        application.getBean(Module2.class, "dependencyOnModule1", Object.class)
        .toString(), is("Module 2 dependency on Module 1 exposed bean"));
  }

  @Test public void moduleName_configured() {
    assertThat(application.getBean(Module1.class, "testmodule", Object.class),
        is(not(nullValue())));
  }

  @Test public void moduleName_default() {
    assertThat(
        application.getBean(Module2.class, "applicationTest.Module2",
            Object.class), is(not(nullValue())));
  }

  @Test public void staticTest() throws Exception {
    String endpoint = baseUrl + "/testmodule/static/static-test.html";

    String page;
    page = executor.execute(Request.Get(endpoint)).returnContent().asString();
    assertThat(page, containsString("static content"));
  }

  @Test public void module2Controller() throws Exception {
    String endpoint = baseUrl + "/applicationTest.Module2/hi.html";
    String page;
    page = executor.execute(Request.Get(endpoint)).returnContent().asString();
    assertThat(page, is("Module 1 exposed bean, 1, 2"));
  }

  @Test public void converter() throws Exception {
    String endpoint = baseUrl + "/applicationTest.Module2/hi.html"
        + "?when=2020-12-20T23:59:59.999-05:00";
    String page;
    page = executor.execute(Request.Get(endpoint)).returnContent().asString();
    assertThat(page, is("Module 1 exposed bean, 1, 2"));
  }

  @Test public void home() throws Exception {
    String page;
    page = executor.execute(Request.Get(baseUrl)).returnContent().asString();
    assertThat(page, containsString("static content"));
  }

  @Test public void webjar() throws Exception {
    String endpoint = baseUrl + "/webjars/jquery/2.2.4/jquery.min.js";
    String page;
    page = executor.execute(Request.Get(endpoint)).returnContent().asString();
    assertThat(page, containsString("jQuery v2.2.4"));
  }

  // Tests that k2 calls init on modules that implement Module.
  @Test public void init() {
    assertThat(initCalled, is(true));
  }

  @Test public void moduleConfiguration() {
    assertThat(application.getBean(Module1.class, "configuration",
        Object.class).toString(),
        is("option1 from module applicationTest.Module2\n"
            + "option1 from module applicationTest.Module3\n"));
  }

  @Test public void propertyOverride() {
    WithProperties withProperties = application.getBean(
        Module1.class, "withProperties", WithProperties.class);
    assertThat(withProperties.getValue1(), is("Overriden value 1"));
    assertThat(withProperties.getValue2(), is("Initial value 2"));
  }

  @Test public void propertyOverride_moduleWithDot() {
    WithProperties withProperties = application.getBean(
        Module2.class, "withProperties", WithProperties.class);
    assertThat(withProperties.getValue1(), is("Initial value 1"));
    assertThat(withProperties.getValue2(), is("Overriden value 2"));
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

  public static class Module1Registry {

    private String configuration;
    private ModuleDefinition requestor;

    Module1Registry(final ModuleDefinition theRequestor) {
      requestor = theRequestor;
    }

    public void setOption(final String value) {
      configuration = value;
    }

    String getOption() {
      return configuration + " from module " + requestor.getModuleName();
    }
  };

  /* A class with properties, to test the PropertyOverrideConfigurer.
   */
  public static class WithProperties {

    private String value1;
    private String value2;

    public void setValue1(final String value) {
      value1 = value;
    }

    public String getValue1() {
      return value1;
    }

    public void setValue2(final String value) {
      value2 = value;
    }

    public String getValue2() {
      return value2;
    }
  };

  // A module named testmodule that exposes a bean named exposedBean.
  @Component("testmodule")
  @Module(relativePath = "../k2-core/src/test/resources")
  public static class Module1 implements RegistryFactory {

    private List<Module1Registry> registries = new LinkedList<>();

    @Override
    public Module1Registry getRegistry(final ModuleDefinition requestor) {
      Module1Registry registry = new Module1Registry(requestor);
      registries.add(registry);
      return registry;
    }

    @Bean public StringHolder testBean() {
      return new StringHolder("Module 1 private bean");
    }

    @Public @Bean public StringHolder exposedBean() {
      return new StringHolder("Module 1 exposed bean");
    }

    @Public @Bean(name = "renamedExposedBean")
    public StringHolder exposedBean2() {
      return new StringHolder("Module 1 renamed exposed bean");
    }

    @Bean public String configuration() {
      String result = "";
      for (Module1Registry registry : registries) {
        result += registry.getOption() + "\n";
      }
      return result;
    }

    @Bean public WithProperties withProperties() {
      WithProperties result = new WithProperties();
      result.setValue1("Initial value 1");
      result.setValue2("Initial value 2");
      return result;
    }
  };

  @Controller
  public static class Module2Controller {

    private String response;

    public Module2Controller(final StringHolder theResponse) {
      response = theResponse.toString();
    }

    @RequestMapping(value = "/hi.html", method = RequestMethod.GET)
    public HttpEntity<String> hi(
        @RequestParam(name = "when", required = false)
        final OffsetDateTime when) {
      return new HttpEntity<String>(response);
    }
  };

  /* Filter that adds a suffix to the response. */
  public static class SuffixFilter extends GenericFilterBean {

    private String suffix;

    SuffixFilter(final String theSuffix) {
      suffix = theSuffix;
    }

    @Override
    public void doFilter(final ServletRequest request,
        final ServletResponse response,
        final FilterChain chain) throws IOException, ServletException {

      ContentCachingResponseWrapper buffer
          = new ContentCachingResponseWrapper((HttpServletResponse) response);

      chain.doFilter(request,  buffer);
      buffer.getOutputStream().print(suffix);
      buffer.copyBodyToResponse();
    }
  };

  // A module named applicationTest.Module2 (the generated name) that exposes a
  // bean named exposedBean and has a bean that depends on
  // testmodule.exposedBean.
  @PropertySource("classpath:com/k2/core/module2.properties")
  public static class Module2 implements Registrator {

    public void addRegistrations(final ModuleContext moduleContext) {
      initCalled = true;
      Module1Registry configuration = moduleContext.get(Module1Registry.class);
      configuration.setOption("option1");
    }

    @Bean public StringHolder testBean() {
      return new StringHolder("Module 2 private bean");
    }

    @Public @Bean public StringHolder exposedBean() {
      return new StringHolder("Module 2 exposed bean");
    }

    @Bean public StringHolder dependencyOnModule1(
        @Qualifier("testmodule.exposedBean") final StringHolder name) {
      return new StringHolder("Module 2 dependency on " + name.toString());
    }

    @Bean public Module2Controller controller2(
        @Qualifier("testmodule.exposedBean") final StringHolder response) {
      return new Module2Controller(response);
    }

    @Bean public FilterRegistrationBean<Filter> suffix1Filter() {
      FilterRegistrationBean<Filter> filter;
      filter = new FilterRegistrationBean<Filter>(new SuffixFilter(", 1"));
      filter.setName("suffix1");
      filter.setOrder(2);
      return filter;
    }

    @Bean public FilterRegistrationBean<Filter> suffix2Filter() {
      FilterRegistrationBean<Filter> filter;
      filter = new FilterRegistrationBean<Filter>(new SuffixFilter(", 2"));
      filter.setName("suffix2");
      filter.setOrder(1);
      return filter;
    }

    @Bean public WithProperties withProperties() {
      WithProperties result = new WithProperties();
      result.setValue1("Initial value 1");
      result.setValue2("Initial value 2");
      return result;
    }
  };

  public static class Module3 implements Registrator {

    public void addRegistrations(final ModuleContext moduleContext) {
      initCalled = true;
      Module1Registry configuration;
      configuration = moduleContext.get(Module1Registry.class);
      configuration.setOption("option1");
    }
  }

  // A web test application with 3 test modules.
  @Configuration
  public static class WebApplication extends Application {

    public WebApplication() {
      super(new Module1(), new Module2(), new Module3());
      setLandingUrl("/testmodule/static/static-test.html");
    }

    public static void main(final String... args) {
      Application webApplication = new WebApplication();
      webApplication.run(args);
    }

    @Bean public String globalBean() {
      return "Global bean";
    }
  }

  // A stand alone test application.
  @Configuration
  public static class StandAloneApplication extends Application {
    private String option = "configOption";
    public StandAloneApplication() {
      super(new Module1());
      setWebEnvironment(false);
    }

    @Bean public String option() {
      return option;
    }
  }

  // A test application with no modules.
  @Configuration
  public static class EmptyApplication extends Application {
    private String option = "configOption";

    @Bean public String option() {
      return option;
    }
  }
}

