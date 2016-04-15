/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.Validate;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config
  .ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;

/** Publishes beans marked with @Public in the module into the global
 * application context.
 *
 * This is must be registered as a BeanFactoryPostProcessor in the module
 * application context.
 *
 * This class creates a singleton proxy in the global context for each bean
 * marked as public in the module context. It names the new bean in the global
 * context as [moduleName].[privateBeanName].
 */
public class ModuleBeansPublisher implements BeanFactoryPostProcessor {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(
      ModuleBeansPublisher.class);

  /** The the application context wrapped by the global application
   * context, initially null.
   */
  private ConfigurableListableBeanFactory parentBeanFactory;

  /** The module name, never null */
  private String moduleName;

  /** The module class, never null. */
  private ModuleDefinition moduleDefinition;

  /** The name of the methods marked with the @Public annotation in the module
   * configuration.
   *
   * This is never null, but it may be empty if the module does not expose any
   * public bean.
   */
  private List<String> publicBeanMethodNames = new LinkedList<String>();

  /** Constructor, creates a ModuleBeansPublisher.
   *
   * @param context the global application context. It cannot be null.
   * @param moduleName the module name. It cannot be null.
   *
   * @param moduleClass the class that specifies the module. It cannot be
   * null.
   */
  ModuleBeansPublisher(final ConfigurableApplicationContext context,
      final String theModuleName, final ModuleDefinition theDefinition) {

    Validate.notNull(context,
        "The global application context cannot be null.");
    Validate.notNull(theModuleName, "The module name cannot be null.");
    Validate.notNull(theDefinition, "The module definition cannot be null.");

    moduleName = theModuleName;
    moduleDefinition = theDefinition;

    log.trace("Entering ModuleBeansPublisher");

    // Finds all the public beans in the module configuration class. This looks
    // for methods annotated with @Public.
    publicBeanMethodNames.addAll(moduleDefinition.getPublicBeanMethodNames());

    // Hack to find the application context wrapped by the global application
    // context. This initializes the parentBeanFactory attribute.
    context.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor(){
      @Override
      public void postProcessBeanFactory(
          final ConfigurableListableBeanFactory beanFactory)
              throws BeansException {
        parentBeanFactory = beanFactory;
      }
    });

    log.trace("Leaving ModuleBeansPublisher");
  }

  /** {@inheritDoc}
   *
   * Looks for all public beans in the module's application context and creates
   * a singleton proxy in the parent context.
   */
  @Override
  public void postProcessBeanFactory(
      final ConfigurableListableBeanFactory beanFactory) throws BeansException {

    log.trace("Entering postProcessBeanFactory");

    beanFactory.registerSingleton("k2.moduleDefinition", moduleDefinition);
    int count = 0;
    for (Object configurer : moduleDefinition.getRegistries()) {
      ++count;
      String name =  "k2.moduleConfiguration" + count;
      beanFactory.registerSingleton(name, configurer);
    }

    BeanDefinitionRegistry beanRegistry = (BeanDefinitionRegistry) beanFactory;

    List<String> publicBeanNames = new LinkedList<String>();

    for (String beanName : beanRegistry.getBeanDefinitionNames()) {
      BeanDefinition definition = beanRegistry.getBeanDefinition(beanName);
      // Checks if the bean is created by one of the @Public methods.
      if (publicBeanMethodNames.contains(definition.getFactoryMethodName())) {
        publicBeanNames.add(beanName);
      }
    }

    for (String publicBeanName : publicBeanNames) {
      ProxyFactoryBean publicBeanProxy = new ProxyFactoryBean();
      publicBeanProxy.setBeanFactory(beanFactory);
      publicBeanProxy.setTargetName(publicBeanName);
      if (!Modifier.isFinal(beanFactory.getType(publicBeanName).getModifiers())) {
        // The ProxyFactoryBean cannot proxy final classes directly.
        publicBeanProxy.setProxyTargetClass(true);
      }
      String publishedBeanName = moduleName + "." + publicBeanName;
      log.debug("Exposing public bean {}", publishedBeanName);
      parentBeanFactory.registerSingleton(publishedBeanName, publicBeanProxy);
    }

    log.trace("Leaving postProcessBeanFactory");
  }
}

