/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.apache.commons.lang3.Validate;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support
    .AnnotationConfigWebApplicationContext;

import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support
    .PropertySourcesPlaceholderConfigurer;

/** A k2 application.
 *
 * A k2 application wraps a spring boot application, that itself wraps a spring
 * application context. K2 creates a hierarchy of application contexts with
 * spring boot application context at its root. Each module configures its own
 * application context that has the root as its parent. K2 also creates a
 * spring DispatcherServet whose application context parent is the module's
 * application context. So, for each dispatcher servlet you have:
 *
 * Root context <-- module context <-- servlet context.
 *
 * The module life cycle:
 *
 * A module goes through three steps in their life:
 *
 * 1- Module registration. This step creates the module registry with the
 * module configuration api.
 *
 * 2- Module initialization. Instantiates module classes and invokes init on
 * each module.
 *
 * 3- Module creation. Creates the application context for each module and
 * wires their respective spring beans.
 */
public class Application {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(Application.class);

  /** The list of modules added to this application, never null.*/
  private final List<Class<?>> moduleClasses;

  /** The spring boot application, null if not yet created.
   *
   * This is initialized with getApplication().
   */
  private SpringApplication application = null;

  /** Indicates if this is a web application, defaults to true.
   *
   * If this is true, then k2 will embed jetty as a web application.
   */
  private boolean isWebEnvironment = true;

  /** The application context that spring boot creates when ran.
   *
   * This is null until the client executes run.
   */
  private ConfigurableApplicationContext applicationContext;

  /** The module definitions off all modules initialized in this application.
   *
   * This is never null.
   */
  private Map<Class<?>, ModuleDefinition> modules
      = new LinkedHashMap<Class<?>, ModuleDefinition>();

  /** Creates a new Application with the given modules.
   *
   * @param modules the list of modules to bootstrap, cannot be null.
   */
  @SafeVarargs
  protected Application(final Class<?> ... modules) {
    Validate.notNull(modules, "The modules cannot be null");
    moduleClasses = Arrays.asList(modules);

    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
  }

  /** Determines if this is a web application.
   *
   * If true, k2 will initialize jetty as an embedded web server. Can only be
   * called before run().
   *
   * @param isWeb true if this is a web application, false otherwise.
   */
  public void setWebEnvironment(final boolean isWeb) {
    Validate.isTrue(application == null,
        "setWebEnviroment cannot be called after run(...).");
    isWebEnvironment = isWeb;
  }

  /** Launches the application.
   *
   * This operation is not synchronized, so take to call it just once, as soon
   * as your program starts. The best way is to create a launcher class with a
   * single static main that creates and runs the k2 application.
   *
   * @param args the command line arguments. It cannot be null.
   */
  public void run(final String[] args) {
    log.trace("Entering run");
    applicationContext = getApplication().run(args);
    log.trace("Leaving run");
  }

  /** Stops the k2 application.
   */
  public void stop() {
    if (applicationContext != null && applicationContext.isActive()) {
      SpringApplication.exit(applicationContext, new ExitCodeGenerator[0]);
    }
    applicationContext = null;
  }

  /** Obtains a bean from the application context of the module specified by
   * moduleClass.
   *
   * This can only be called after run().
   *
   * @param moduleClass the class that configured the module. It cannot be
   * null. It must correspond to a registered module.
   *
   * @param beanName the name of the bean to obtain. It cannot be null. The
   * module application context must have a bean with this name.
   *
   * @return the bean named beanName in the corresponding module. Never returns
   * null.
   */
  public Object getBean(final Class<?> moduleClass, final String beanName) {
    Validate.notNull(application, "You must call run before this operation");
    ModuleDefinition definition = modules.get(moduleClass);
    Validate.notNull(definition, "The module "
        + moduleClass.getSimpleName() + " was not found.");
    return definition.getContext().getBean(beanName);
  }

  /** Obtains a bean from the global application context.
   *
   * This can only be called after run().
   *
   * @param beanName the name of the bean to obtain. It cannot be null. The
   * global application context must have a bean with this name.
   *
   * @return the bean named beanName in the global application context. Never
   * returns null.
   */
  public Object getBean(final String beanName) {
    Validate.notNull(application, "You must call run before this operation");
    return applicationContext.getBean(beanName);
  }

  /** Obtains the configuration api for each module that implements.
   */
  private void initializeModules() {
    for (Class<?> moduleClass : moduleClasses) {
      modules.put(moduleClass, new ModuleDefinition(moduleClass));
    }
  }

  /** Calls init on all modules that implement Module.
   */
  private void registerModules() {
    for (ModuleDefinition definition : modules.values()) {
      Registrator module = definition.getModuleInitializer();
      if (module != null) {
        ModuleContext moduleContext;
        moduleContext = new ModuleContext(definition, modules.values());
        module.addRegistrations(moduleContext);
      }
    }
  }

  /** Returns the wrapped spring boot application, creating one if not already
   * created.
   *
   * @return a spring boot application, never returns null.
   */
  SpringApplication getApplication() {

    log.trace("Entering getApplication");

    if (application == null) {
      log.debug("Creating new k2 application");

      initializeModules();
      registerModules();

      List<Class<?>> applicationConfigurations = new LinkedList<Class<?>>();
      if (isWebEnvironment) {
        applicationConfigurations.add(WebConfiguration.class);
      }
      applicationConfigurations.add(getClass());
      application = new SpringApplication(applicationConfigurations.toArray());
      application.setBannerMode(Banner.Mode.OFF);
      application.setWebEnvironment(isWebEnvironment);

      // Adds an initializer that registers the modules in the spring boot
      // application.
      application.addInitializers(
        new ApplicationContextInitializer<ConfigurableApplicationContext>() {
        /** {@inheritDoc} */
        @Override
        public void initialize(
            final ConfigurableApplicationContext parentContext) {
          for (ModuleDefinition moduleDefinition : modules.values()) {
            createModule(parentContext, moduleDefinition);
          }
        }
      });

      // Adds a listener that refreshes all the module contexts.
      application.addListeners(
          new ApplicationListener<ContextRefreshedEvent>() {
        /** {@inheritDoc} */
        @Override
        public void onApplicationEvent(final ContextRefreshedEvent event) {
          if (event.getApplicationContext().getParent() == null) {
            for (ModuleDefinition definition : modules.values()) {
              definition.getContext().refresh();
            }
          }
        }
      });

    }
    log.trace("Leaving getApplication");
    return application;
  }

  /** Registers the module.
   *
   * This creates a spring application context with the module beans, exposes
   * the public beans and creates a dispatcher servlet.
   *
   * @param context the global application context. It cannot be null.
   *
   * @param moduleClass the module to register. It cannot be null.
   */
  private void createModule(final ConfigurableApplicationContext context,
      final ModuleDefinition definition) {

    log.trace("Entering registerModule {}", definition.getModuleName());

    String moduleName = definition.getModuleName();

    // Creates a new web application context initialized with the moduleClass.
    AnnotationConfigWebApplicationContext moduleContext;
    moduleContext = definition.getContext();
    moduleContext.setParent(context);
    moduleContext.addBeanFactoryPostProcessor(
        new ModuleBeansPublisher(context, moduleName, definition));

    if (this.isWebEnvironment) {
      registerDispatcherServlet(context, moduleName, moduleContext);
    }

    log.trace("Leaving registerModule");
  }

  /** Creates a spring dispatcher servlet and registers it in the provided
   * context.
   *
   * This creates a dispatcher servlet for the module with the provided name. It
   * registers it in context, and sets moduleContext as its parent.
   *
   * @param context the context where this operation will register the
   * dispatcher servlet. It cannot be null.
   *
   * @param moduleName the name of the module. It cannot be null.
   *
   * @param moduleContext the module spring context. It cannot be null.
   */
  private void registerDispatcherServlet(
      final ConfigurableApplicationContext context, final String moduleName,
      final AnnotationConfigWebApplicationContext moduleContext) {

    Validate.notNull(context, "The application context cannot be null.");
    Validate.notNull(moduleName, "The module name cannot be null.");
    Validate.notNull(moduleContext, "The module context cannot be null.");

    // Create a new application context for the dispatcher servlet and
    // initialze its parent. The dispatcher will do its magic on the context and
    // call refresh.
    AnnotationConfigWebApplicationContext servletContext;
    servletContext = new AnnotationConfigWebApplicationContext();
    servletContext.register(DispatcherServletContext.class);
    servletContext.setParent(moduleContext);

    DispatcherServlet dispatcherServlet;
    dispatcherServlet = new DispatcherServlet(servletContext);

    BeanDefinitionBuilder builder = BeanDefinitionBuilder
        .rootBeanDefinition(ServletRegistrationBean.class);
    builder.addConstructorArgValue(dispatcherServlet);
    builder.addConstructorArgValue("/" + moduleName + "/*");
    builder.addPropertyValue("name", moduleName);
    builder.setLazyInit(true);

    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context;
    registry.registerBeanDefinition(moduleName, builder.getBeanDefinition());
  }

  /** Bean factory post processor to support @Value in spring beans.
   *
   * This lets modules use @Value in their global bean configuration.
   *
   * @return a post processor that can interpret @Value annotations,
   * never null.
   */
  @Bean public PropertySourcesPlaceholderConfigurer
      propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}

