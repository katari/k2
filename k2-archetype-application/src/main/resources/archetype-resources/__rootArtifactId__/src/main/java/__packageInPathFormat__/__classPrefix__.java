#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package};

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.Validate;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.k2.core.Registrator;
import com.k2.core.ModuleContext;
import com.k2.swagger.SwaggerRegistry;

import ${package}.swagger.${classPrefix}ApiController;
import ${package}.application.${classPrefix}Delegate;

/** The ${classPrefix} module.
 */
@Component("${classPrefix.toLowerCase()}")
public class ${classPrefix} implements Registrator {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(${classPrefix}.class);

  /** Registers the idl to the swagger module.
   */
  @Override
  public void addRegistrations(final ModuleContext moduleContext) {

    SwaggerRegistry swaggerRegistry = moduleContext.get(SwaggerRegistry.class);
    Validate.notNull(swaggerRegistry, "Swagger not found in app");

    swaggerRegistry.registerIdl("/${classPrefix.toLowerCase()}/static/api.yaml");
  }

  /////////////////////////////////////////////////////////////////////////////
  //// Application level related beans.
  /////////////////////////////////////////////////////////////////////////////

  /** The ${classPrefix} delegate.
   *
   * @return the ${classPrefix} delegate, never null.
   */
  @Bean public ${classPrefix}Delegate ${classPrefix.toLowerCase()}Delegate() {
    return new ${classPrefix}Delegate();
  }

  /** The ${classPrefix.toLowerCase()} api controller.
   *
   * @param ${classPrefix.toLowerCase()}Delegate the
   * ${classPrefix.toLowerCase()} delegate. It cannot be null.
   *
   * @return the ${classPrefix.toLowerCase()} api controller, never null.
   */
  @Bean public ${classPrefix}ApiController ${classPrefix.toLowerCase()}ApiController(
      final ${classPrefix}Delegate ${classPrefix.toLowerCase()}Delegate) {
    return new ${classPrefix}ApiController(${classPrefix.toLowerCase()}Delegate);
  }

  /** A simple controller registered as a bean.
   *
   * @return the controller, never null.
   */
  @Bean public HelloController helloController() {
    return new HelloController();
  }
}

