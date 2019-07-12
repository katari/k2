/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

/** Defines a module that adds registrations to another module.
 *
 * Modules implements this interface if they need to register some information
 * in another module. A good example is a module that needs to register
 * persistent classes in hibernate.
 */
public interface Registrator {

  /** Called by the k2 application so that modules can register information
   * in another module.
   *
   * Modules may decide to implement a sort of 'registry' that other modules
   * use to hook additional configuration into it. The module context provides
   * such implementations through the 'get' operation (see ModuleContext.get).
   *
   * Modules may do something like:
   *
   * moduleContext.get(HibernateRegistry.class).addPersistentClasses(...);
   *
   * @param moduleContext the module context, used to look up for registration
   * implementations. It cannot be null.
   */
  void addRegistrations(ModuleContext moduleContext);
}

