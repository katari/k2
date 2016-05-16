/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The shiro registry.
 *
 * Through the shiro registry, modules register their endpoints to shiro with
 * the permissions necessary to access that endpoint.
 */
public class ShiroRegistry {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(ShiroRegistry.class);

  /** The name of the module using this registry, never null.
   */
  private String moduleName;

  /** The shiro module, never null.
   */
  private Shiro shiroModule;

  /** Constructor, creates a shiro regitry.
   *
   * @param theModuleName the name of the module using this registry. It cannot
   * be null.
   *
   * @param shiro the shiro module. It cannot be null.
   */
  ShiroRegistry(final String theModuleName, final Shiro shiro) {
    moduleName = theModuleName;
    shiroModule = shiro;
  }

  /** Registers the shiro login page.
   *
   * Only one module can register the login url.
   *
   * @param url the module relative url that will provide the login page. It
   * cannot be null.
   */
  public void registerLoginUrl(final String url) {
    Validate.notNull(url, "The login url cannot be null.");
    String contextRelativeUrl = getContextRelativeUrl(url);
    shiroModule.registerLoginUrl(contextRelativeUrl);
    registerEndpoint(contextRelativeUrl, "authc");
  }

  /** Registers and enpoint with its permission.
   *
   * Examples of filter chains:
   *
   * ssl, authc
   *
   * anon
   *
   * authc, perms['remote:invoke']
   *
   * @param pattern the module relative url path pattern to match against a
   * request url. It cannot be null.
   *
   * @param filterChain a string representation of the shiro filter chain to
   * use to check if the user can access the provided url. It cannot be null.
   */
  public void registerEndpoint(final String pattern, final String filterChain) {
    log.trace("Entering registerEndpoint");
    String urlPattern = getContextRelativeUrl(pattern);
    log.debug("Adding {} -> {}", urlPattern, filterChain);
    shiroModule.addChainDefinition(urlPattern, filterChain);
    log.trace("Leaving registerEndpoint");
  }

  /** Obtains the context relative url from a module relative url.
   *
   * @param moduleRelativeUrl The module relative url. It cannot be null.
   *
   * @return the context relative url, never returns null.
   */
  private String getContextRelativeUrl(final String moduleRelativeUrl) {
    String result = "/" + moduleName;
    if (!moduleRelativeUrl.startsWith("/")) {
      result += "/";
    }
    result += moduleRelativeUrl;
    return result;
  }
}

