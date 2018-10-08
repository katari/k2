/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.Validate;

import org.springframework.beans.factory.config
    .ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context
    .AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/** The application context that k2 applications create in a web environment.
 *
 * AnnotationConfigEmbeddedWebApplicationContext that initializes and refreshes
 * the application context of all the modules in a k2 application, and
 * registers their servlets and filters.
 */
class K2WebApplicationContext
      extends AnnotationConfigServletWebServerApplicationContext {

  /** The instantiation strategy to use the k2 application instance instead of
   * its class as a spring configuration.
   */
  private K2InstantiationStrategy instantiationStrategy;

  /** The modules declared in the k2 application, never null. */
  private Collection<ModuleDefinition> modules;

  /** Constructor, creates a new instance.
   *
   * @param theInstance the instance of the configuration. It cannot be null.
   *
   * @param theModules the list of modules to initialize. It cannot be null.
   */
  K2WebApplicationContext(final Object theInstance,
      final Collection<ModuleDefinition> theModules) {
    Validate.notNull(theInstance, "The instance cannot be null");
    Validate.notNull(theModules, "The modules cannot be null");
    modules = theModules;
    instantiationStrategy = new K2InstantiationStrategy(null, theInstance);
  }

  /** {@inheritDoc}.
   *
   * This is a very dirty hack: we need to include the module's servlet context
   * initializers in the list of the parent context initializers. As spring
   * boot does not provide a 'pre-servlet-container-started' hook, we need to
   * refresh the modules as side effect of this operation.
   */
  @Override
  protected Collection<ServletContextInitializer>
      getServletContextInitializerBeans() {

    ServletContext servletContext = getServletContext();

    List<ServletContextInitializer> initializers = new LinkedList<>();

    initializers.addAll(super.getServletContextInitializerBeans());

    for (ModuleDefinition module : modules) {
      module.refresh(this.getBeanFactory(), servletContext);
      initializers.addAll(module.getServletContextInitializers());
    }

    Collections.sort(initializers, AnnotationAwareOrderComparator.INSTANCE);

    return initializers;
  }

  /** Obtains a new bean factory.
   *
   * This is overriden to load bean definitions from a k2 application instance.
   * It uses the K2InstantiationStrategy.
   */
  @Override
  protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    DefaultListableBeanFactory beanFactory;
    beanFactory = (DefaultListableBeanFactory) super.obtainFreshBeanFactory();

    beanFactory.setInstantiationStrategy(instantiationStrategy);

    return beanFactory;
  }
}

