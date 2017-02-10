/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;

/** A spring environment implementation that can provide a list of property
 * values.
 *
 * This lets you configure beans in your module using a map. You simply make
 * your bean depend on K2Environment and call getProperties(prefix) to obtain
 * all the properties with that prefix.
 *
 * For an example, look k2 hibernate module, that uses this to configure the
 * session factory. Hibernate does not provide a 'configuration' pojo that can
 * be initialized by spring from a standard environment, hibernate provides an
 * applySettings operation that takes a map as parameter. So to initialize it,
 * the module calls applySettings(environment.getProperties("hibernate")).
 */
public class K2Environment extends StandardServletEnvironment {

  /** The class logger. */
  private Logger log = LoggerFactory.getLogger(K2Environment.class);

  /** Constructor, creates a new K2Enviroment copying all the property sources
   * and active profiles.
   *
   * @param environment the environment to copy the information from. It cannot
   * be null.
   */
  K2Environment(final ConfigurableEnvironment environment) {
    Validate.notNull(environment, "The environment cannot be null.");
    removeAllPropertySources(getPropertySources());
    setActiveProfiles(environment.getActiveProfiles());
    for (PropertySource<?> source : environment.getPropertySources()) {
      getPropertySources().addLast(source);
    }
  }

  /** Removes all the sources from the provided property sources.
   *
   * @param sources the property sources. It cannot be null.
   */
  private void removeAllPropertySources(final MutablePropertySources sources) {
    Validate.notNull(sources, "The property sources cannot be null.");
    Set<String> names = new HashSet<String>();
    for (PropertySource<?> propertySource : sources) {
      names.add(propertySource.getName());
    }
    for (String name : names) {
      sources.remove(name);
    }
  }

  /** Returns all the properties that start with the specified prefix.
   *
   * This is a convenience method that calls
   *
   * getProperties(prefix, false)
   *
   * @param prefix the prefix that the name of the property must start with to
   * be added to the resulting Properties.  It cannot be null nor empty.
   *
   * @return a Properties instance, never returns null.
   */
  public Properties getProperties(final String prefix) {
    return getProperties(prefix, false);
  }

  /** Returns all the properties that start with the specified prefix.
   *
   * This operation only considers enumerable property sources, so it ignores
   * jndi and other sources. The prefix defines which properties to include in
   * the result. For example, if there are properties x.a, y.a and xb, a call
   * to getProperties("x") will return a Properties that only contain the name
   * "x.a", ie: the prefix is followed by a '.'.
   *
   * @param prefix the prefix that the name of the property must start with to
   * be added to the resulting Properties. It cannot be null nor empty.
   *
   * @param removePrefix if true, the names of the returned properties does not
   * include the prefix. Otherwise, the prefix and the '.' are removed from the
   * property names.
   *
   * @return a Properties instance, never returns null.
   */
  public Properties getProperties(final String prefix,
      final boolean removePrefix) {

    Validate.notEmpty(prefix, "prefix cannot be null nor empty");
    log.trace("Entering getProperties({})", prefix);

    Properties properties = new Properties();
    for (PropertySource<?> propertySource : getPropertySources()) {
      if (propertySource instanceof EnumerablePropertySource<?>) {
        EnumerablePropertySource<?> source;
        source = (EnumerablePropertySource<?>) propertySource;
        for (String name : source.getPropertyNames()) {
          if (name.startsWith(prefix + ".")) {
            String propertyName;
            if (removePrefix) {
              propertyName = name.substring(prefix.length() + 1);
            } else {
              propertyName = name;
            }
            properties.setProperty(propertyName, this.getProperty(name));
          }
        }
      } else {
        log.debug("{} is not enumerable, ignoring", propertySource.getName());
      }
    }
    log.trace("Leaving getProperties({})", prefix);

    return properties;
  }
}

