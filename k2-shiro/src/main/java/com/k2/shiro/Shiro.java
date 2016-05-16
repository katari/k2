/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import java.util.Map;
import java.util.LinkedHashMap;

import javax.servlet.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.Validate;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;

import com.k2.core.ModuleDefinition;
import com.k2.core.RegistryFactory;

/** This module integrates shiro in k2 applications.
 *
 * To use this module you need to create a module that provides the login url
 * (see ShiroRegistry) and a public bean that implements a shiro realm, or
 * provide your realm implementation in your application.
 *
 * This module maintains all session information in a browser cookie, so do not
 * abuse session information.
 *
 * It exposes a /logout endponint that mainly deletes the session cookie.
 */
@Component("shiro")
public class Shiro implements RegistryFactory {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(Shiro.class);

  /** The login url registered by at most one module.
   *
   * Null if there is no login form configured in the application.
   */
  private String loginUrl = null;

  /** The shiro filter chain definition, a map of url patters to filters to
   * apply to that url.
   *
   * This is written by the ShiroRegistry when a module calls registerEndpoint.
   */
  private final Map<String, String> chainDefinitions = new LinkedHashMap<>();

  /** Registers the shiro login page.
   *
   * @param url the context relative url that will provide the login page. It
   * cannot be null.
   */
  void registerLoginUrl(final String url) {
    Validate.isTrue(loginUrl == null,
        "Only one module can register the login url.");
    loginUrl = url;
  }

  /** Adds a new chain definition to the list of shiro chain definitions.
   *
   * See ShiroRegistry.registerEndpoint for more information.
   *
   * @param pattern the url pattern. It cannot be null.
   *
   * @param chain the shiro filter chain. It cannot be null.
   */
  void addChainDefinition(final String pattern, final String chain) {
    if (chainDefinitions.isEmpty()) {
      chainDefinitions.put("/logout", "saveSession, noSessionCreation, logout");
    }
    chainDefinitions.put(pattern, "saveSession, noSessionCreation, " + chain);
  }

  /** Creates a shiro registry for the provided module. */
  @Override
  public ShiroRegistry getRegistry(final ModuleDefinition requestor) {
    log.trace("Entering getRegistry({})", requestor.getModuleName());
    ShiroRegistry result;
    result = new ShiroRegistry(requestor.getModuleName(), this);
    log.trace("Leaving getRegistry()");
    return result;
  }

  /** The security manager.
   *
   * @param realm the realm used by the security manager. This in intended to
   * be configured in the K2 application as a spring bean. The name is
   * irrelevant, it will be matched by type. It cannot be null.
   *
   * @return the security manager, never null.
   */
  @Bean public SecurityManager webSecurityManager(final Realm realm) {
    Validate.notNull(realm,
        "The realm cannot be null. Create one in your application.");
    DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
    securityManager.setRealm(realm);
    securityManager.setSubjectFactory(new K2SubjectFactory());
    return securityManager;
  }

  /** Creates the main shiro filter that intercepts every call to the web
   * application.
   *
   * @param securityManager the shiro security manager to use in this shiro
   * filter. It cannot be null.
   *
   * @return a properly initialized shiro filter, set up to filter all
   * requests. Never returns null.
   */
  @Bean public FilterRegistrationBean shiroFilter(
      final SecurityManager securityManager) {
    ShiroFilterFactoryBean filterFactory = new ShiroFilterFactoryBean();
    filterFactory.setSecurityManager(securityManager);

    filterFactory.getFilters().put("saveSession", new SaveSessionFilter());

    addChainDefinition("/**", "authc");
    filterFactory.setFilterChainDefinitionMap(chainDefinitions);

    filterFactory.setLoginUrl(loginUrl);

    Filter filter;
    try {
      filter = ((Filter) filterFactory.getObject());
    } catch (Exception e) {
      throw new RuntimeException("Error creating shiro filter.", e);
    }

    FilterRegistrationBean registration = new FilterRegistrationBean(filter);
    registration.setName("shiroFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);

    return registration;
  }
}

