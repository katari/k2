/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.Validate;

import java.beans.Introspector;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support
    .AnnotationConfigWebApplicationContext;

/** Holds all the information to manage a module and its life cycle.
 */
class ModuleDefinition {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(ModuleDefinition.class);

  /** The module class, basically a spring configuration class.
   *
   * This is never null.
   */
  private Class<?> moduleClass;

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

  /** Constructor, creates a new module definition.
   *
   * @param theModuleClass the spring configuration that represents the
   * module. This class may implement ModuleInitializer and ModuleInformation.
   */
  ModuleDefinition(final Class<?> theModuleClass) {
    Validate.notNull(theModuleClass, "The module class cannot be null");
    moduleClass = theModuleClass;
  }

  /** Returns an instance of the module registry factory if the module
   * implements that interface.
   *
   * @return an instance of the registry factory, or null if the module does
   * not implement RegistryFactory.
   */
  RegistryFactory getRegistryFactory() {
    if (RegistryFactory.class.isAssignableFrom(moduleClass)) {
      return (RegistryFactory) getInstance();
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

  /** Returns all the registries created by this module.
   *
   * @return the registries, never returns null.
   */
  Collection<Object> getRegistries() {
    return registries.values();
  }

  /** Returns an instance of the module initializer if the module implements
   * that interface.
   *
   * @return an instance of the module initializer, or null if the module does
   * not implement ModuleInitializer.
   */
  Registrator getModuleInitializer() {
    if (Registrator.class.isAssignableFrom(moduleClass)) {
      return (Registrator) getInstance();
    }
    return null;
  }

  /** Returns an instance of the module class.
   *
   * This creates just one instance, no matter how many times it is called.
   *
   * @return the instance, never null.
   */
  private Object getInstance() {
    if (moduleInstance == null) {
      try {
        moduleInstance  = moduleClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        throw new RuntimeException("Error creating module", e);
      }
    }
    return moduleInstance;
  }

  /** Returns the application context initialized from the module class.
   *
   * The returned context is not properly initialized. The k2 application
   * is the responsible for fully initializing and refreshing the returned
   * application context.
   *
   * This creates just one instance, no matter how many times it is called.
   *
   * @return a spring application context, never null.
   */
  AnnotationConfigWebApplicationContext getContext() {
    if (context == null) {
      context = new AnnotationConfigWebApplicationContext();
      context.register(moduleClass);
    }
    return context;
  }

  /** Determines the module name.
   *
   * Module writers can specify the module name using the @Component spring
   * annotation. If that annotation is not found or it does not specify a
   * name, findModuleName uses the class name to derive a module name.
   *
   * @return a string with the module name.
   */
  String getModuleName() {
    Component component = moduleClass.getAnnotation(Component.class);
    String name = null;
    if (component != null) {
      name = component.value();
    }
    if (name == null || "".equals(name)) {
      name = ClassUtils.getShortName(moduleClass);
      name = Introspector.decapitalize(name);
    }
    return name;
  }

  /** Returns the list of methods in the module configuration that creates
   * public beans.
   *
   * This looks for methods with the @Public annotation.
   *
   * @return a list of method names, never null.
   */
  List<String> getPublicBeanMethodNames() {
    log.trace("Entering getPublicBeanMethodNames");
    List<String> result = new LinkedList<String>();
    for (Method method : moduleClass.getMethods()) {
      if (AnnotationUtils.findAnnotation(method, Public.class) != null) {
        result.add(method.getName());
        log.debug("Found @Public method {}.", method.getName());
      }
    }
    log.trace("Leaving getPublicBeanMethodNames");
    return result;
  }
}

