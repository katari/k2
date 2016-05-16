/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import org.apache.commons.lang3.Validate;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support
    .CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.RootBeanDefinition;

/** Loads bean definitions from an instance of a spring configuration.
 *
 * Normally, spring creates bean definitions from a configuration class, and
 * spring itself instantiates that class. With this class you can instantiate
 * and initialize the configuration, then let spring load its bean definitions
 * from that instance.
 */
class K2InstantiationStrategy
    extends CglibSubclassingInstantiationStrategy {

  /** The bean name of the bean to provide as the instance.
   *
   * If this is null, it matches the instance by class.
   */
  private String instanceBeanName;

  /** The instance to return as a bean, never null. */
  private Object instance;

  /** Creates the instantiation strategy.
   *
   * @param theBeanName the name of the bean to provide as the instance. If
   * null, this instantiator will return the instance based on the class name.
   *
   * @param theInstance the instance to return as a spring bean. It cannot be
   * null.
   */
  K2InstantiationStrategy(final String theBeanName, final Object theInstance) {
    Validate.notNull(theInstance, "The instance cannot be null.");
    instanceBeanName = theBeanName;
    instance = theInstance;
  }

  /** Returns the provided instance if the requested bean matches the name or,
   * if the name is null, the instance class.
   */
  @Override
  public Object instantiate(final RootBeanDefinition beanDefinition,
      final String beanName, final BeanFactory owner) {

    Object result;
    if (instanceBeanName != null && instanceBeanName.equals(beanName)) {
      result = instance;
    } else if (instanceBeanName == null
        && instance.getClass().equals(beanDefinition.getBeanClass())) {
      result = instance;
    } else {
      result = super.instantiate(beanDefinition, beanName, owner);
    }
    return result;
  }
}

