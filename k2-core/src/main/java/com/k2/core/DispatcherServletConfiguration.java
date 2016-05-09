/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import org.springframework.web.servlet.config.annotation
    .ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation
    .ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation
    .WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation
    .RequestMappingHandlerMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support
    .PropertySourcesPlaceholderConfigurer;

/** Custom configuration for the dispatcher servlet application context
 *
 * K2 uses this configuration to initialize the dispatcher servlet if the
 * application runs in a web environment
 * {@see Application#setWebEnvironment(boolean)}.
 */
@Configuration
public class DispatcherServletConfiguration extends WebMvcConfigurationSupport {

  /** Whether the application is loaded in debug modue or not.
   *
   * Injected by spring.
   *
   * NOTE: Avoid @Value in attributes as plage in your code. I could not
   * find another way, given that this is used in an overriden operation from
   * WebMvcConfigurationSupport.
   */
  private @Value(value = "${debug:false}") boolean debug;

  /** The module definition that owns the DispatcherServlet of this
   * configuration.
   *
   * Injected by spring, never null after wired.
   *
   * NOTE: Avoid Autowired in attributes as plague in your code. I could not
   * find another way, given that this is used in an overriden operation from
   * WebMvcConfigurationSupport.
   */
  private @Autowired ModuleDefinition moduleDefinition;

  /** Defines a handler mapping to look for handlers in the current
   * application context and its parent.
   *
   * @return a properly configured handler mapping. Never returns null.
   */
  @Override
  protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
    RequestMappingHandlerMapping result = new RequestMappingHandlerMapping();
    result.setDetectHandlerMethodsInAncestorContexts(true);
    return result;
  }

  /** Registers handlers that serve the module static content.
   */
  @Override
  protected void addResourceHandlers(final ResourceHandlerRegistry registry) {
    ResourceHandlerRegistration reg;
    reg = registry.addResourceHandler("/static/**");
    if (debug && moduleDefinition.getRelativePath() != null) {
      reg.addResourceLocations("file:" + moduleDefinition.getRelativePath()
          + "/" + moduleDefinition.getStaticPath());
    }
    reg.addResourceLocations("classpath:" + moduleDefinition.getStaticPath());
  }

  /** Bean to resolve @Value from k2 environment.
   *
   * @return a place holder configurer, to resolve @Value. Never returns null.
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigIn() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}

