/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger;

import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

import com.k2.core.ModuleDefinition;

public class SwaggerControllerTest {

  /** The class logger. */
  private static Logger log = LoggerFactory.getLogger(
      SwaggerControllerTest.class);

  private ModuleDefinition moduleDefinition;
  private SwaggerRegistry registry1;
  private SwaggerRegistry registry2;

  @Before public void setUp() {
    moduleDefinition = mock(ModuleDefinition.class);

    registry1 = mock(SwaggerRegistry.class);
    registry2 = mock(SwaggerRegistry.class);
  }

  @Test public void swaggerUi_noRegistries() {

    List<SwaggerRegistry> registries = new LinkedList<>();

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi().getBody();

    assertThat(body, containsString("No idl found"));
  }

  @Test public void swaggerUi_registriesWithoutIdl() {

    List<SwaggerRegistry> registries = Arrays.asList(registry1, registry2);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi().getBody();

    assertThat(body, containsString("No idl found"));
  }

  @Test public void swaggerUi_oneRegistry() {

    when(registry1.getIdl()).thenReturn("/module1/api.yaml");
    when(registry1.getRequestorPath()).thenReturn("module1");

    List<SwaggerRegistry> registries = Arrays.asList(registry1);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi().getBody();

    assertThat(body, containsString("/module1/api.yaml"));
    assertThat(body, containsString("\"module1\""));
  }

  @Test public void swaggerUi_twoRegistries() {

    when(registry1.getIdl()).thenReturn("/module1/api.yaml");
    when(registry1.getRequestorPath()).thenReturn("module1");

    when(registry2.getIdl()).thenReturn("/module2/api.yaml");
    when(registry2.getRequestorPath()).thenReturn("module2");

    List<SwaggerRegistry> registries = Arrays.asList(registry1, registry2);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi().getBody();

    assertThat(body, containsString("/module1/api.yaml"));
    assertThat(body, containsString("\"module1\""));

    assertThat(body, containsString("/module2/api.yaml"));
    assertThat(body, containsString("\"module2\""));
  }

  @Test public void swaggerUi_debugLoadsFromFs() {
    when(registry1.getIdl()).thenReturn("/module1/api.yaml");
    when(registry1.getRequestorPath()).thenReturn("module1");

    when(moduleDefinition.getRelativePath()).thenReturn("src/test/html");

    List<SwaggerRegistry> registries = Arrays.asList(registry1);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, true);

    String body = controller.swaggerUi().getBody();

    assertThat(body, containsString("From file system"));
  }

  @Test public void swaggerUi_debugLoadsFromClasspath() {
    when(registry1.getIdl()).thenReturn("/module1/api.yaml");
    when(registry1.getRequestorPath()).thenReturn("module1");

    when(moduleDefinition.getRelativePath()).thenReturn("NON-EXISTING-DIR");

    List<SwaggerRegistry> registries = Arrays.asList(registry1);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, true);

    String body = controller.swaggerUi().getBody();

    assertThat(body, containsString("/module1/api.yaml"));
    assertThat(body, containsString("\"module1\""));
  }
}

