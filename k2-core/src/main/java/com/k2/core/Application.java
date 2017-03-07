/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

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
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support
    .AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support
    .PropertySourcesPlaceholderConfigurer;

/** A k2 application.
 *
 * A k2 application wraps a spring boot application, that itself wraps a spring
 * application context. K2 creates a hierarchy of application contexts with
 * spring boot application context at its root. Each module configures its own
 * application context that has the root as its parent. In a web environment, K2
 * also creates a spring DispatcherServet whose application context parent is
 * the module's application context. So, for each module you have:
 *
 * Root context --- module context --- servlet context.
 *
 * Module writers may optionally implement two interfaces: Registrator and
 * RegistryFactory. A registrator is a module that uses other modules registries
 * to register something into that module. K2 calss Registrator.addRegistrations
 * on each module that implement Registrator before creating the spring
 * application context of each module.
 *
 * The counterpart of Registrator is the RegistryFactory. Modules that implement
 * RegistryFactory provides registries for modules that implement Registrator.
 * K2 Hibernate module is a sample of a RegistryFactory.
 *
 * To register a module, application writers must create an instance of each
 * module and call the application contructor.
 */
public class Application {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(Application.class);

  /** The module definitions off all modules initialized in this application.
   *
   * This is never null.
   */
  private Map<Class<?>, ModuleDefinition> modules
      = new LinkedHashMap<Class<?>, ModuleDefinition>();

  /** The spring boot application, null if not yet created.
   *
   * This is initialized with getApplication().
   */
  private SpringApplication springApplication = null;

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

  /** The landing url, never null.
   */
  private String landingUrl = "";

  /** Creates a new Application with the given modules.
   *
   * @param moduleInstances the list of modules to bootstrap, cannot be null.
   */
  @SafeVarargs
  protected Application(final Object ... moduleInstances) {
    Validate.notNull(modules, "The modules cannot be null");
    for (Object moduleInstance : moduleInstances) {
      modules.put(moduleInstance.getClass(),
          new ModuleDefinition(moduleInstance));
    }

    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
  }

  /** Determines if this is a web application.
   *
   * If true, k2 will initialize jetty as an embedded web server. Can only be
   * called before run(). K2 assumes that is running a web application by
   * default.
   *
   * @param isWeb true if this is a web application, false otherwise.
   */
  public void setWebEnvironment(final boolean isWeb) {
    Validate.isTrue(springApplication == null,
        "setWebEnviroment cannot be called after run(...).");
    isWebEnvironment = isWeb;
  }

  /** Sets the landing url.
   *
   * @param theLandingUrl the landing url, it cannot be null.
   */
  public void setLandingUrl(final String theLandingUrl) {
    Validate.notNull(theLandingUrl, "The landing url cannot be null.");
    landingUrl = theLandingUrl;
  }

  /** The landing url.
   *
   * @return the landing url, never null, "" if the landing url was not
   * configured.
   */
  @Bean(name = "k2.landingUrl") public String landingUrl() {
    return landingUrl;
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
    Validate.notNull(springApplication, "You must call run before getBean");
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
    Validate.notNull(springApplication, "You must call run before getBean");
    return applicationContext.getBean(beanName);
  }

  /** Calls addRegistrations on all modules that implement Registrator.
   */
  private void registerModules() {
    for (ModuleDefinition definition : modules.values()) {
      Registrator registrator = definition.getModuleRegistator();
      if (registrator != null) {
        ModuleContext moduleContext;
        moduleContext = new ModuleContext(definition, modules.values());
        registrator.addRegistrations(moduleContext);
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

    if (springApplication == null) {
      log.debug("Creating new k2 application");

      SpringApplication app;

      registerModules();

      List<Class<?>> applicationConfigurations = new LinkedList<Class<?>>();
      if (isWebEnvironment) {
        applicationConfigurations.add(WebConfiguration.class);
      }
      applicationConfigurations.add(getClass());
      app = new SpringApplication(applicationConfigurations.toArray()) {

        /** {@inheritDoc}
         *
         * Overriden to change the type of application context to use,
         * K2WebApplicationContext or K2ApplicationContext, depending on
         * isWebEnvironment.
         */
        @Override
        protected ConfigurableApplicationContext createApplicationContext() {
          if (isWebEnvironment) {
            return new K2WebApplicationContext(Application.this,
                modules.values());
          } else {
            return new K2ApplicationContext(Application.this,
                modules.values());
          }
        }
      };
      app.setBannerMode(Banner.Mode.OFF);
      app.setWebEnvironment(isWebEnvironment);

      configureInitializers(app);

      springApplication = app;
    }
    log.trace("Leaving getApplication");
    return springApplication;
  }

  /** Configures the spring boot application initializers that create the
   * K2Environment and initialize the module application contexts.
   *
   * The K2 application needs to configure the environment in the first
   * initialization step, then let spring boot run its own initializer, and
   * finally run the initializer that creates the module contexts.
   *
   * @param app the spring boot application context, never null.
   */
  private void configureInitializers(final SpringApplication app) {
    Validate.notNull(app, "The spring boot application cannot be null");

    List<ApplicationContextInitializer<?>> initializers = new LinkedList<>();

    // Add the environment initializer.
    initializers.add(
      new ApplicationContextInitializer<ConfigurableApplicationContext>() {
      /** {@inheritDoc}
       *
       * Creates the K2Environment and puts it in the application context. */
      @Override
      public void initialize(final ConfigurableApplicationContext parent) {
        parent.setEnvironment(new K2Environment(parent.getEnvironment()));
      }
    });

    // Adds the default spring boot initializers.
    initializers.addAll(app.getInitializers());

    // Adds an initializer that registers the modules in the spring boot
    // application.
    initializers.add(
      new ApplicationContextInitializer<ConfigurableApplicationContext>() {
      /** {@inheritDoc}
       *
       *  Creates the spring application context for each module. */
      @Override
      public void initialize(final ConfigurableApplicationContext parent) {
        // Add the post processor to support @Value in the spring
        // configuration.
        PropertySourcesPlaceholderConfigurer placeHolderConfigurer =
            new PropertySourcesPlaceholderConfigurer();
        placeHolderConfigurer.setEnvironment(parent.getEnvironment());
        parent.addBeanFactoryPostProcessor(placeHolderConfigurer);

        if (isWebEnvironment) {
          registerRootDispatcherServlet(parent);
        }

        // Add the application context for each module.
        for (ModuleDefinition moduleDefinition : modules.values()) {
          createModule(parent, moduleDefinition);
        }
      }
    });

    app.setInitializers(initializers);
  }

  /** Registers the module.
   *
   * Creates a spring application context with the module beans and creates a
   * dispatcher servlet in a web environment. This operation does not refresh
   * the application context of any module.
   *
   * @param parent the global application context. It cannot be null.
   *
   * @param definition the definition of the module to register. It cannot be
   * null.
   */
  private void createModule(final ConfigurableApplicationContext parent,
      final ModuleDefinition definition) {

    log.trace("Entering createModule {}", definition.getModuleName());

    Validate.notNull(parent, "The parent context cannot be null.");
    Validate.notNull(definition, "The module definiton cannot be null.");

    String moduleName = definition.getModuleName();

    // Creates a new web application context initialized with the moduleClass.
    AnnotationConfigWebApplicationContext moduleContext;
    moduleContext = definition.getContext();
    moduleContext.setParent(parent);

    if (isWebEnvironment) {
      registerDispatcherServlet(parent, moduleName, moduleContext);
    }

    log.trace("Leaving createModule");
  }

  /** Registers a dispatcher servlet to serve webjars from the /webjars path.
   *
   * @param context the application root application context. It cannot be null.
   */
  private void registerRootDispatcherServlet(
      final ConfigurableApplicationContext context) {
    Validate.notNull(context, "The application context cannot be null.");

    AnnotationConfigWebApplicationContext servletContext;
    servletContext = new AnnotationConfigWebApplicationContext();
    servletContext.setParent(context);

    servletContext.register(RootDispatcherServletConfiguration.class);

    DispatcherServlet dispatcherServlet;
    dispatcherServlet = new DispatcherServlet(servletContext);

    BeanDefinitionBuilder builder = BeanDefinitionBuilder
        .rootBeanDefinition(ServletRegistrationBean.class);
    builder.addConstructorArgValue(dispatcherServlet);
    builder.addConstructorArgValue("/*");
    builder.addPropertyValue("name", "k2.rootDispatcherServlet");
    builder.setLazyInit(true);

    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context;
    registry.registerBeanDefinition("k2-root", builder.getBeanDefinition());
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
    // set the module context as its parent. The dispatcher servlet will do its
    // magic on the context and call refresh.
    AnnotationConfigWebApplicationContext servletContext;
    servletContext = new AnnotationConfigWebApplicationContext();
    servletContext.register(DispatcherServletConfiguration.class);
    servletContext.setParent(moduleContext);

    DispatcherServlet dispatcherServlet;
    dispatcherServlet = new DispatcherServlet(servletContext);

    BeanDefinitionBuilder builder = BeanDefinitionBuilder
        .rootBeanDefinition(ServletRegistrationBean.class);
    builder.addConstructorArgValue(dispatcherServlet);
    builder.addConstructorArgValue("/" + moduleName + "/*");
    builder.addPropertyValue("name", moduleName);
    builder.addPropertyValue("order", 0);
    builder.setLazyInit(true);

    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context;
    registry.registerBeanDefinition(moduleName, builder.getBeanDefinition());
  }
}

