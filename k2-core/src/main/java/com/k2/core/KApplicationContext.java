/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.Collection;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation
    .AnnotationConfigApplicationContext;

/** The application context that k2 applications create in a non web
 * environment.
 *
 * AnnotationConfigApplicationContext that initializes and refreshes the
 * application context of all the modules in a k2 application.
 */
class K2ApplicationContext extends AnnotationConfigApplicationContext {

  /** The modules declared in the k2 application, never null. */
  private Collection<ModuleDefinition> modules;

  /** Constructor, creates a new instance.
   *
   * @param theModules the list of modules to initialize. It cannot be null.
   */
  K2ApplicationContext(final Collection<ModuleDefinition> theModules) {
    Validate.notNull(theModules, "The modules cannot be null");
   modules = theModules;
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
};

