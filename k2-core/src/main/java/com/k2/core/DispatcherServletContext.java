/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import org.springframework.web.servlet.config.annotation
    .WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation
    .RequestMappingHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Custom configuration for the dispatcher servlet application context
 *
 * K2 uses this configuration to initialize the dispatcher servlet if the
 * application runs in a web environment
 * {@see Application#setWebEnvironment(boolean)}.
 */
@Configuration
public class DispatcherServletContext extends WebMvcConfigurationSupport {

  /** Defines a handler mapping to look for handlers in the current
   * application context and its parent.
   *
   * @return a properly configured handler mapping. Never returns null.
   */
  @Bean public RequestMappingHandlerMapping handlerMapping() {
    RequestMappingHandlerMapping handlerMapping;
    handlerMapping = new RequestMappingHandlerMapping();
    handlerMapping.setDetectHandlerMethodsInAncestorContexts(true);
    return handlerMapping;
  }
}

