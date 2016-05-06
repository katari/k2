/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.Validate;
import org.springframework.boot.context.embedded
    .AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/** The application context that k2 applications create in a web environment.
 *
 * AnnotationConfigEmbeddedWebApplicationContext that initializes and refreshes
 * the application context of all the modules in a k2 application, and
 * registers their servlets and filters.
 */
class K2WebApplicationContext
      extends AnnotationConfigEmbeddedWebApplicationContext {

  /** The modules declared in the k2 application, never null. */
  private Collection<ModuleDefinition> modules;

  /** Constructor, creates a new instance.
   *
   * @param theModules the list of modules to initialize. It cannot be null.
   */
  K2WebApplicationContext(final Collection<ModuleDefinition> theModules) {
    Validate.notNull(theModules, "The modules cannot be null");
    modules = theModules;
  }

  /** {@inheritDoc}
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
}

