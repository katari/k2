/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.LinkedHashMap;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.Validate;

import java.beans.Introspector;
import java.lang.reflect.Method;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.context.properties
    .EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support
    .AnnotationConfigWebApplicationContext;

/** Holds all the information to manage a module and its life cycle.
 */
public class ModuleDefinition {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(ModuleDefinition.class);

  /** The module instance.
   *
   * This is a cache of an instance of moduleClass, used to avoid creating
   * multiple instances of the module class.
   *
   * This is null until somebody calls getInstance.
   */
  private Object moduleInstance = null;

  /** The spring application context initialized from the moduleClass.
   *
   * This is initialized in getContext, null if that operation is not called.
   */
  private AnnotationConfigWebApplicationContext context = null;

  /** All the registries created by this module, never null.
   */
  private Map<ModuleDefinition, Object> registries = new LinkedHashMap<>();

  /** A list of all the public beans in this module.
   *
   * This is used by the k2 application to export this module public beans.
   */
  private List<String> publicBeanNames = new LinkedList<String>();

  /** Constructor, creates a new module definition.
   *
   * @param module the module instance. It cannot be null.
   */
  ModuleDefinition(final Object module) {
    Validate.notNull(module, "The module cannot be null");
    moduleInstance = module;
  }

  /** Determines the module name.
   *
   * Module writers can specify the module name using the @Component spring
   * annotation. If that annotation is not found or it does not specify a
   * name, findModuleName uses the class name to derive a module name.
   *
   * @return a string with the module name.
   */
  public String getModuleName() {
    Class<?> moduleClass = moduleInstance.getClass();

    String name = null;
    Component component = moduleClass.getAnnotation(Component.class);
    if (component != null) {
      name = component.value();
    }
    if (name == null) {
      Configuration config = moduleClass.getAnnotation(Configuration.class);
      if (config != null) {
        name = config.value();
      }
    }
    if (name == null || "".equals(name)) {
      name = ClassUtils.getShortName(moduleClass);
      name = Introspector.decapitalize(name);
    }
    return name;
  }

  /** Returns the module short name, as specified by the @Module annotation.
   *
   * @return the module short name, or null if not specified.
   */
  public String getModuleShortName() {
    Class<?> moduleClass = moduleInstance.getClass();

    String shortName = null;
    Module module = moduleClass.getAnnotation(Module.class);
    if (module != null && !Module.NO_SHORT_NAME.equals(module.shortName())) {
      shortName = module.shortName();
    }
    return shortName;
  }

  /** Obtains a bean of the specified type from the application context of this
   * module.
   *
   * @param <T> the type of bean to return.
   *
   * @param type the type of bean to obtain. It cannot be null.
   *
   * @return a bean of the specified type, never returns null.
   */
  public <T> T getBean(final Class<T> type) {
    return context.getBean(type);
  }

  /** Returns the classpath root relative location of the static content
   * served by this module.
   *
   * K2 will expose the static content found in a subpackage called 'static'
   * of the module package. The static content will be available in the url
   * [base]/[module-name]/static/.
   *
   * @return a string of the form [package as path]/static/.
   */
  String getStaticPath() {
    return moduleInstance.getClass().getPackage().getName().replace(".", "/")
        + "/static/";
  }

  /** The file system relative path where to find this module resources.
   *
   * This is used to support reloading of static resources when they are
   * modified during development. In debug mode, k2 will look for resources
   * first in the file system based on this path, and then in their normal
   * location in the classpath.
   *
   * @return the file system relative path of this module resources. null if
   * the module does support reloading of resources during development.
   */
  public String getRelativePath() {
    Class<?> moduleClass = moduleInstance.getClass();

    String path = null;
    Module module = moduleClass.getAnnotation(Module.class);
    if (module != null && !module.relativePath().isEmpty()) {
      path = module.relativePath();
    }
    return path;
  }

  /** Returns an instance of the module registry factory if the module
   * implements RegistryFactory.
   *
   * @return an instance of the registry factory, or null if the module does
   * not implement RegistryFactory.
   */
  RegistryFactory getRegistryFactory() {
    if (moduleInstance instanceof RegistryFactory) {
      return (RegistryFactory) moduleInstance;
    }
    return null;
  }

  /** Obtains a registry for the provided module.
   *
   * @param requestor the module definition of the module that will use the
   * registry. This is never null.
   *
   * @return a module registry for the provided module, or null if this module
   * does not implement RegistryFactory.
   */
  Object getRegistry(final ModuleDefinition requestor) {

    Object registry = registries.get(requestor);
    if (registry == null) {
      RegistryFactory registryFactory = getRegistryFactory();
      if (registryFactory != null) {
        registry = registryFactory.getRegistry(requestor);
        registries.put(requestor, registry);
      }
    }
    return registry;
  }

  /** Returns an instance of the module initializer if the module implements
   * that interface.
   *
   * @return an instance of the module initializer, or null if the module does
   * not implement ModuleInitializer.
   */
  Registrator getModuleRegistator() {
    if (moduleInstance instanceof Registrator) {
      return (Registrator) moduleInstance;
    }
    return null;
  }

  /** A synthetic class to introduce the EnableConfigurationProperties
   * annotation in the module.
   *
   * EnableConfigurationProperties simply imports
   * EnableConfigurationPropertiesImportSelector in the application context,
   * but that class is package access, so I cannot not add it directly.
   */
  @EnableConfigurationProperties
  private static class AnnotationHolder {}

  /** Returns the application context initialized from the module instance.
   *
   * The returned context is not properly initialized. The k2 application
   * is the responsible for fully initializing and refreshing the returned
   * application context.
   *
   * Once the module application context is refreshed, the context contains the
   * module definition under the name k2.moduleDefinition.
   *
   * This creates just one instance, no matter how many times it is called.
   *
   * @return a spring application context, never null.
   */
  AnnotationConfigWebApplicationContext getContext() {
    if (context == null) {

      context = new AnnotationConfigWebApplicationContext() {
        @Override
        protected void loadBeanDefinitions(
            final DefaultListableBeanFactory beanFactory) {
          beanFactory.setInstantiationStrategy(
              new K2InstantiationStrategy(getModuleName(), moduleInstance));
          super.loadBeanDefinitions(beanFactory);
        }
      };

      final K2Environment environment;
      environment = new K2Environment(context.getEnvironment());
      context.setEnvironment(environment);
      context.register(AnnotationHolder.class);
      context.register(moduleInstance.getClass());

      // Resolves ${...} in @Value annotations.
      PropertySourcesPlaceholderConfigurer placeHolderConfigurer =
          new PropertySourcesPlaceholderConfigurer();
      placeHolderConfigurer.setEnvironment(context.getEnvironment());
      context.addBeanFactoryPostProcessor(placeHolderConfigurer);

      // Overrides bean properties from values obtained from the spring
      // environment. We cannot directly pass the K2Environment properties to
      // the PropertyOverrideConfigurer because it is not yet initialized here.
      PropertyOverrideConfigurer overrideConfigurer;
      overrideConfigurer = new PropertyOverrideConfigurer() {
        /** {@inheritDoc} */
        @Override
        protected Properties mergeProperties() throws IOException {
          Properties properties = super.mergeProperties();
          CollectionUtils.mergePropertiesIntoMap(environment.getProperties(
                getModuleName(), true), properties);
          return properties;
        }
      };
      overrideConfigurer.setIgnoreInvalidKeys(true);
      context.addBeanFactoryPostProcessor(overrideConfigurer);

      // Exposes the public beans.
      context.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
        /** {@inheritDoc} */
        @Override
        public void postProcessBeanFactory(
            final ConfigurableListableBeanFactory beanFactory) {
          beanFactory.registerSingleton("k2.moduleDefinition",
              ModuleDefinition.this);
          BeanDefinitionRegistry beanRegistry;
          beanRegistry = (BeanDefinitionRegistry) beanFactory;
          recordPublicBeanNames(beanRegistry);
        }

      });
    }
    return context;
  }

  /** Refreshes the wrapped application context.
   *
   * This operation refreshes the application context initializing the servlet
   * related features, and exports the public beans to the parent bean factory.
   *
   * @param parentBeanFactory the bean factory to export the public beans to.
   * It cannot be null.
   *
   * @param servletContext the context of the web application context that is
   * running this application. Null if the application is not running in a web
   * environment.
   */
  void refresh(final ConfigurableListableBeanFactory parentBeanFactory,
      final ServletContext servletContext) {
    Validate.notNull(parentBeanFactory,
        "The parent bean factory cannot be null");
    if (servletContext != null) {
      getContext().setServletContext(servletContext);
    }
    getContext().refresh();
    exportPublicBeans(parentBeanFactory);
  }

  /** Closes the module and its corresponding application context.
   */
  void close() {
    getContext().close();
  }

  /** Returns a list of servlet context initializers registered in the module.
   *
   * A module writer may add private servlets and filters to his module, and
   * ask K2 to register them in the servlet container by creating
   * ServletRegistrationBean or FilterRegistrationBean beans (or any kind of
   * ServletContextInitializer).
   *
   * If you want to specify the order of your initializers, do not add @Order
   * to the bean definition, it is ignored. You need to set the order attribute
   * in the RegistrationBean.
   *
   * @return all the context initializers registered in the current module,
   * never returns null.
   */
  List<ServletContextInitializer> getServletContextInitializers() {
    List<ServletContextInitializer>  result = new LinkedList<>();

    // Obtains all the ServletContextInitializer instances in the module
    result.addAll(getContext().getBeansOfType(ServletContextInitializer.class,
        false, false).values());

    return result;
  }

  /** Export all the beans that have been recorded as public in
   * recordPublicBeanNames.
   *
   * This registers a singleton in the provided parent bean factory with the
   * public bean instance. The exported bean is of the form:
   *
   * [module-name].[local-bean-name]
   *
   * This is called from k2 application at the end of the refresh process of
   * the global context.
   *
   * @param parentBeanFactory the bean factory to register the public beans. It
   * cannot be null.
   */
  private void exportPublicBeans(
      final ConfigurableListableBeanFactory parentBeanFactory) {
    log.trace("Entering exportPublicBeans");
    Validate.notNull(parentBeanFactory,
        "The parent bean factory cannot be null");
    for (String publicBeanName : publicBeanNames) {
      String publishedBeanName = getModuleName() + "." + publicBeanName;
      log.debug("Exposing bean {} as {}", publicBeanName, publishedBeanName);
      parentBeanFactory.registerSingleton(publishedBeanName,
          context.getBean(publicBeanName));
    }
    log.trace("Leaving exportPublicBeans");
  }

  /** Finds all the beans that should be exported to the global application
   * context.
   *
   * This is called during module context refresh through a bean factory post
   * processor. It initializes the publicBeanNames attribute.
   *
   * @param beanRegistry the bean registry that contains all the bean
   * definitions.
   */
  private void recordPublicBeanNames(
      final BeanDefinitionRegistry beanRegistry) {
    List<String> publicBeanMethodNames;
    publicBeanMethodNames = getPublicBeanMethodNames();
    for (String beanName : beanRegistry.getBeanDefinitionNames()) {
      BeanDefinition definition = beanRegistry.getBeanDefinition(beanName);
      // Checks if the bean is created by one of the @Public methods.
      if (publicBeanMethodNames.contains(definition.getFactoryMethodName())) {
        publicBeanNames.add(beanName);
      }
    }
  }

  /** Returns the list of methods in the module configuration that creates
   * public beans.
   *
   * This looks for methods with the @Public annotation.
   *
   * @return a list of method names, never null.
   */
  private List<String> getPublicBeanMethodNames() {
    log.trace("Entering getPublicBeanMethodNames");
    List<String> result = new LinkedList<String>();
    for (Method method : moduleInstance.getClass().getMethods()) {
      if (AnnotationUtils.findAnnotation(method, Public.class) != null) {
        result.add(method.getName());
        log.debug("Found @Public method {}.", method.getName());
      }
    }
    log.trace("Leaving getPublicBeanMethodNames");
    return result;
  }
}

