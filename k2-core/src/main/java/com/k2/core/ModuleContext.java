/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** The context that k2 presents to a module during the registration phase.
 *
 * Modules use this class to look for other modules registration
 * implementations.
 */
public class ModuleContext {

  /** The module definition of the module that k2 asks to register its
   * information, never null.
   */
  private ModuleDefinition definition;

  /** The module definitions of all the modules in the application, never
   * null.
   */
  private Collection<ModuleDefinition> modules;

  private Map<Class<?>, Object> registries = new LinkedHashMap<>();

  /** Constructor, creates a module context for a specific module.
   *
   * @param theModules the list of modules in the application. It cannot be
   * null.
   *
   * @param theDefinition the module definition wrapped in this module
   * context. It cannot be null.
   */
  ModuleContext(final ModuleDefinition theDefinition,
      final Collection<ModuleDefinition> theModules) {
    definition = theDefinition;
    modules = theModules;
  }

  /** Obtains a registry of a specific type.
   *
   * @param registryType the type of registry to find.
   *
   * @return an implementation of the provided type, or null if none found.
   */
  @SuppressWarnings("unchecked")
  <T> T get(final Class<T> registryType) {

    T registry = (T) registries.get(registryType);

    if (registry == null) {
      for (ModuleDefinition definitionForProvider : modules) {
        Object configurer;
        configurer = definitionForProvider.getRegistry(definition);
        if (registryType.isInstance(configurer)) {
          registry = (T) configurer;
          registries.put(registryType, registry);
          break;
        }
      }
    }
    return registry;
  }
}

