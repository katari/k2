/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger;

import org.apache.commons.lang3.Validate;

import com.k2.core.ModuleDefinition;

/** The swagger registry.
 *
 * This stores the url of a module swagger spec.
 */
public class SwaggerRegistry {

  /** The definition of the module that is registering its swagger
   * specification, never null.
   */
  private ModuleDefinition requestor;

  /** The swagger spec of a module.
   *
   * This should be set by a module by calling registerIdl.
   */
  private String swaggerIdl;

  /** Creates a new swagger registry.
   *
   * @param theRequestor the module requesting the registry. It cannot be null.
   */
  public SwaggerRegistry(final ModuleDefinition theRequestor) {
    Validate.notNull(theRequestor, "The requestor cannot be null.");
    requestor = theRequestor;
  }

  /** Registers a swagger idl file of a module.
   *
   * @param idlUrl the module relative url of the idl file. It cannot be null.
   */
  public void registerIdl(final String idlUrl) {
    Validate.isTrue(swaggerIdl == null, "Only one idl per module.");
    Validate.notNull(idlUrl, "The idl url cannot be null.");
    swaggerIdl = idlUrl;
  }

  /** Returns the module provided idl url.
   *
   * @return the url, not null if the module called registerIdl.
   */
  String getIdl() {
    return swaggerIdl;
  }

  /** Returns the url base path of the requesting module.
   *
   * @return the base path, never returns null.
   */
  String getRequestorPath() {
    return requestor.getModuleName();
  }
}

