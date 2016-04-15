/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

/** Interface to mark a k2 module as a registry factory.
 *
 * Module writes can implement this interface in their modules to expose
 * registries that other modules can use to register information.
 */
public interface RegistryFactory {

  /** Obtains a registry for the provided module.
   *
   * @param requestor the module definition of the module that is requesting
   * the registry, never null.
   *
   * @return a registry for the provided module, never null.
   */
  <T> T getRegistry(final ModuleDefinition requestor);
}

