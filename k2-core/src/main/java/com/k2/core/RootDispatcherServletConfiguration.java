package com.k2.core;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation
    .ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation
    .WebMvcConfigurationSupport;

/** A spring configuration class to use in the root dispatcher servlet.
 *
 * This configuration registers a handler to server webjars.
 */
@Configuration
class RootDispatcherServletConfiguration extends WebMvcConfigurationSupport {

  /** Registers handlers that serve the static content from webjars..
   */
  @Override
  protected void addResourceHandlers(
      final ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/");
  }
};
