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

import org.springframework.mock.web.MockHttpServletRequest;

import com.k2.core.ModuleDefinition;

public class SwaggerControllerTest {

  /** The class logger. */
  private static Logger log = LoggerFactory.getLogger(
      SwaggerControllerTest.class);

  private ModuleDefinition moduleDefinition;
  private SwaggerRegistry registry1;
  private SwaggerRegistry registry2;

  private List<String> idls1;
  private List<String> idls2;

  private MockHttpServletRequest request = new MockHttpServletRequest();

  @Before public void setUp() {
    moduleDefinition = mock(ModuleDefinition.class);

    registry1 = mock(SwaggerRegistry.class);
    registry2 = mock(SwaggerRegistry.class);

    idls1 = new LinkedList<String>();
    idls1.add("/module1/api.yaml");

    idls2 = new LinkedList<String>();
    idls2.add("/module2/api.yaml");
  }

  @Test public void swaggerUi_noRegistries() throws Exception {

    List<SwaggerRegistry> registries = new LinkedList<>();

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi(request).getBody();

    assertThat(body, containsString("No idl found"));
  }

  @Test public void swaggerUi_registriesWithoutIdl() throws Exception {

    List<SwaggerRegistry> registries = Arrays.asList(registry1, registry2);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi(request).getBody();

    assertThat(body, containsString("No idl found"));
  }

  @Test public void swaggerUi_oneRegistry() throws Exception {

    when(registry1.getIdls()).thenReturn(idls1);
    when(registry1.getRequestorPath()).thenReturn("module1");

    List<SwaggerRegistry> registries = Arrays.asList(registry1);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi(request).getBody();

    assertThat(body, containsString("/module1/api.yaml"));
    assertThat(body, containsString("\"module1\""));
  }

  @Test public void swaggerUi_twoRegistries() throws Exception {

    when(registry1.getIdls()).thenReturn(idls1);
    when(registry1.getRequestorPath()).thenReturn("module1");

    when(registry2.getIdls()).thenReturn(idls2);
    when(registry2.getRequestorPath()).thenReturn("module2");

    List<SwaggerRegistry> registries = Arrays.asList(registry1, registry2);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, false);

    String body = controller.swaggerUi(request).getBody();

    assertThat(body, containsString("/module1/api.yaml"));
    assertThat(body, containsString("\"module1\""));

    assertThat(body, containsString("/module2/api.yaml"));
    assertThat(body, containsString("\"module2\""));
  }

  @Test public void swaggerUi_debugLoadsFromFs() throws Exception {
    when(registry1.getIdls()).thenReturn(idls1);
    when(registry1.getRequestorPath()).thenReturn("module1");

    when(moduleDefinition.getRelativePath()).thenReturn("src/test/html");

    List<SwaggerRegistry> registries = Arrays.asList(registry1);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, true);

    String body = controller.swaggerUi(request).getBody();

    assertThat(body, containsString("From file system"));
  }

  @Test public void swaggerUi_debugLoadsFromClasspath() throws Exception {
    when(registry1.getIdls()).thenReturn(idls1);
    when(registry1.getRequestorPath()).thenReturn("module1");

    when(moduleDefinition.getRelativePath()).thenReturn("NON-EXISTING-DIR");

    List<SwaggerRegistry> registries = Arrays.asList(registry1);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, true);

    String body = controller.swaggerUi(request).getBody();

    assertThat(body, containsString("/module1/api.yaml"));
    assertThat(body, containsString("\"module1\""));
  }

  @Test public void swaggerUi_multpleIdls() throws Exception {
    idls1.add("/module1/api2.yaml");
    when(registry1.getIdls()).thenReturn(idls1);
    when(registry1.getRequestorPath()).thenReturn("module1");

    List<SwaggerRegistry> registries = Arrays.asList(registry1);

    SwaggerController controller;
    controller = new SwaggerController(moduleDefinition, registries, true);

    String body = controller.swaggerUi(request).getBody();

    assertThat(body, containsString("/module1/api.yaml"));
    assertThat(body, containsString("/module1/api2.yaml"));
    assertThat(body, containsString("\"module1/api\""));
    assertThat(body, containsString("\"module1/api2\""));
  }
}

