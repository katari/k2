/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

import org.springframework.beans.factory.config
    .ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation
    .AnnotationConfigApplicationContext;

/** The application context that k2 applications create in a non web
 * environment.
 *
 * AnnotationConfigApplicationContext that initializes and refreshes the
 * application context of all the modules in a k2 application.
 */
class K2ApplicationContext extends AnnotationConfigApplicationContext {

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
  K2ApplicationContext(final Object theInstance,
      final Collection<ModuleDefinition> theModules) {
    Validate.notNull(theInstance, "The instance cannot be null");
    Validate.notNull(theModules, "The modules cannot be null");
    modules = theModules;
    instantiationStrategy = new K2InstantiationStrategy(null, theInstance);
  }

  /** {@inheritDoc}
   *
   * Refreshes all the modules.
   */
  @Override
  protected void onRefresh() {
    for (ModuleDefinition module : modules) {
      module.refresh(this.getBeanFactory(), null);
    }
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

