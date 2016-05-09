/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import java.io.IOException;
import java.net.URL;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.containsString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class ApplicationTest {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(ApplicationTest.class);

  private static boolean initCalled = false;

  private Application application;

  private String baseUrl = "http://localhost:8081";

  @Before public void setUp() {
    log.trace("Entering setUp");
    initCalled = false;
    application = new WebApplication();
    application.run(new String[0]);
    log.trace("Leaving setUp");
  }

  @After public void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void emptyApplication() {
    application.stop();
    Application emptyApplication = new EmptyApplication();
    emptyApplication.run(new String[0]);
    assertThat(emptyApplication.getApplication(), is(not(nullValue())));
    emptyApplication.stop();
  }

  @Test public void standAloneApplication() {
    application.stop();
    Application standAloneApplication = new StandAloneApplication();
    standAloneApplication.run(new String[0]);
    assertThat(standAloneApplication.getApplication(), is(not(nullValue())));
    assertThat(standAloneApplication.getBean(Module1.class, "testBean")
        .toString(), is("Module 1 private bean"));
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

  @Test public void run_privateBean() {
    assertThat(application.getBean(Module1.class, "testBean").toString(),
        is("Module 1 private bean"));
  }

  @Test public void run_globalBean() {
    assertThat(application.getBean("globalBean").toString(),
        is("Global bean"));
  }

  @Test public void run_exposedBean() {
    assertThat(application.getBean("testmodule.exposedBean")
        .toString(), is("Module 1 exposed bean"));
  }

  @Test public void run_renamedExposedBean() {
    assertThat(application.getBean("testmodule.renamedExposedBean")
        .toString(), is("Module 1 renamed exposed bean"));
  }

  @Test public void run_injectedWithExposedBean() {
    assertThat(
        application.getBean(Module2.class, "dependencyOnModule1")
        .toString(), is("Module 2 dependency on Module 1 exposed bean"));
  }

  @Test public void moduleName_configured() {
    assertThat(application.getBean(Module1.class, "testmodule"),
        is(not(nullValue())));
  }

  @Test public void moduleName_default() {
    assertThat(
        application.getBean(Module2.class, "applicationTest.Module2"),
        is(not(nullValue())));
  }

  @Test public void staticTest() throws Exception {
    String endpoint = baseUrl + "/testmodule/static/static-test.html";
    try (Scanner scanner = new Scanner(new URL(endpoint).openStream())) {
      scanner.useDelimiter("\\A");
      assertThat(scanner.next(), containsString("static content"));
      }
    }

  @Test public void module2Controller() throws Exception {
    String endpoint = baseUrl + "/applicationTest.Module2/hi.html";
    try (Scanner scanner = new Scanner(new URL(endpoint).openStream())) {
      scanner.useDelimiter("\\A");
      assertThat(scanner.next(), is("Module 1 exposed bean, 1, 2"));
    }
  }

  // Tests that k2 calls init on modules that implement Module.
  @Test public void init() {
    assertThat(initCalled, is(true));
  }

  @Test public void moduleConfiguration() {
    assertThat(application.getBean(Module1.class, "configuration").toString(),
        is("option1 from module applicationTest.Module2\n"
            + "option1 from module applicationTest.Module3\n"));
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

  public static class Module1Configurer {

    private String configuration;
    private ModuleDefinition requestor;

    Module1Configurer(final ModuleDefinition theRequestor) {
      requestor = theRequestor;
    }

    public void setOption(final String value) {
      configuration = value;
    }

    String getOption() {
      return configuration + " from module " + requestor.getModuleName();
    }
  };

  // A module named testmodule that exposes a bean named exposedBean.
  @Component("testmodule")
  @Module(relativePath = "../k2-core/src/test/resources")
  public static class Module1 implements RegistryFactory {

    private List<Module1Configurer> configurers = new LinkedList<>();

    @Override
    public Module1Configurer getRegistry(
        final ModuleDefinition requestor) {
      Module1Configurer configurer = new Module1Configurer(requestor);
      configurers.add(configurer);
      return configurer;
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
      for (Module1Configurer configurer : configurers) {
        result += configurer.getOption() + "\n";
      }
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
    public HttpEntity<String> hi() {
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
  public static class Module2 implements Registrator {

    public void addRegistrations(final ModuleContext moduleContext) {
      initCalled = true;
      Module1Configurer configuration;
      configuration = moduleContext.get(Module1Configurer.class);
      configuration.setOption("option1");
    }

    @Bean public StringHolder testBean() {
      return new StringHolder("Module 2 private bean");
    }

    @Public @Bean public StringHolder exposedBean() {
      return new StringHolder("Module 2 exposed bean");
    }

    @Bean public StringHolder dependencyOnModule1(
        @Qualifier("testmodule.exposedBean")
            final StringHolder module1ExposedBean) {
      return new StringHolder("Module 2 dependency on "
            + module1ExposedBean.toString());
    }

    @Bean public Module2Controller controller2(
        @Qualifier("testmodule.exposedBean")
          final StringHolder response) {
      return new Module2Controller(response);
    }

    @Bean public FilterRegistrationBean suffix1Filter() {
      FilterRegistrationBean filter;
      filter = new FilterRegistrationBean(new SuffixFilter(", 1"));
      filter.setName("suffix1");
      filter.setOrder(2);
      return filter;
    }

    @Bean public FilterRegistrationBean suffix2Filter() {
      FilterRegistrationBean filter;
      filter = new FilterRegistrationBean(new SuffixFilter(", 2"));
      filter.setName("suffix2");
      filter.setOrder(1);
      return filter;
    }
  };

  public static class Module3 implements Registrator {

    public void addRegistrations(final ModuleContext moduleContext) {
      initCalled = true;
      Module1Configurer configuration;
      configuration = moduleContext.get(Module1Configurer.class);
      configuration.setOption("option1");
    }
  }

  // A web test application with 3 test modules.
  public static class WebApplication extends Application {

    public WebApplication() {
      super(new Module1(), new Module2(), new Module3());
    }

    public static void main(final String ... args) {
      Application application = new WebApplication();
      application.run(new String[0]);
    }

    @Bean public String globalBean() {
      return "Global bean";
    }
  }

  // A stand alone test application.
  public static class StandAloneApplication extends Application {
    public StandAloneApplication() {
      super(new Module1());
      setWebEnvironment(false);
    }
  }

  // A test application with no modules.
  public static class EmptyApplication extends Application {
  }
}

