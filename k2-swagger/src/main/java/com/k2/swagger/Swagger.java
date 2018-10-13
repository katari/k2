/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.k2.core.Module;
import com.k2.core.ModuleDefinition;
import com.k2.core.RegistryFactory;

/** The swagger module, shows the swagger spec api documentation.
 *
 * Each module may register the endpoint where it exposes the swagger spec. For
 * this, it must implement the Registrator interface and in addRegistrations,
 * call:
 *
 * SwaggerRegistry registry = moduleContext.get(SwaggerRegistry.class);
 *
 * registry.registerIdl("/module1/static/api1.yaml");
 *
 * K2 swagger assumes that module exposes the api from its root.
 */
@Component("swagger")
@Module(relativePath = "../k2-swagger/src/main/resources")
public class Swagger implements RegistryFactory {

  /** The registries requested by all modules, never null.
   */
  private List<SwaggerRegistry> registries = new LinkedList<>();

  /** {@inheritDoc} */
  @Override
  public SwaggerRegistry getRegistry(final ModuleDefinition requestor) {
    SwaggerRegistry result = new SwaggerRegistry(requestor);
    registries.add(result);
    return result;
  }

  /** The swagger controller that shows the spec documentation.
   *
   * @param moduleDefinition the module definition. It cannot be null.
   *
   * @param contextPath the application base path, by default is empty
   *
   * @param debug true if in debug mode.
   *
   * @return the swagger main controller, never returns null.
   */
  @Bean SwaggerController controller(final ModuleDefinition moduleDefinition,
      @Value("${server.contextPath:}") final String contextPath,
      @Value("${debug:#{false}}") final boolean debug) {
    Validate.notNull(registries, "The registries cannot be null.");
    return new SwaggerController(moduleDefinition, registries, contextPath,
        debug);
  }
}

