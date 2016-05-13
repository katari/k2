/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded
  .EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty
  .JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.context.embedded.ServletRegistrationBean;

import org.springframework.context.annotation.Bean;

/** The k2 web configuration that contains the spring beans to configure jetty
 * as the embedded web application.
 */
public class WebConfiguration {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(WebConfiguration.class);

  /** Configures the embedded servlet container implementation.
   *
   * This bean creates and configures a jetty embedded server.
   *
   * @param port the port that jetty will listen on.
   *
   * @param serverCustomizer a the server customizer that configures specific
   * options in jetty. See serverCustomizer(...). It cannot be null.
   *
   * @return the factory, never null.
   */
  @Bean public EmbeddedServletContainerFactory servletContainer(
      @Value("${server.port:8081}") final int port,
      final JettyServerCustomizer serverCustomizer) {

    JettyEmbeddedServletContainerFactory factory =
        new JettyEmbeddedServletContainerFactory("", port);
    //Integer.valueOf(port));
    factory.addServerCustomizers(serverCustomizer);
    factory.addServerCustomizers(forwardedRequestCustomizer());

    return factory;
  }

  /** Jetty server customizer.
   *
   * @param minThreads the minimum number of active threads.
   *
   * @param maxThreads the maximum number of active threads.
   *
   * @param idleTimeOut Set the maximum thread idle time. Threads that are idle
   * for longer than this period may be stopped. Delegated to the named or
   * anonymous Pool.
   *
   * @return a jetty servlet customizer, never returns null.
   */
  @Bean
  public JettyServerCustomizer serverCustomizer(
      @Value("${jetty.minThreads:50}") final String minThreads,
      @Value("${jetty.maxThreads:300}") final String maxThreads,
      @Value("${jetty.idleTime:60000}") final String idleTimeOut) {
    JettyServerCustomizer customizer = new JettyServerCustomizer() {
      @Override
      public void customize(final Server pServer) {
        QueuedThreadPool pool = pServer.getBean(QueuedThreadPool.class);
        pool.setMinThreads(Integer.valueOf(minThreads));
        pool.setMaxThreads(Integer.valueOf(maxThreads));
        pool.setIdleTimeout(Integer.valueOf(idleTimeOut));
      }
    };
    return customizer;
  }

  /** Jetty customizer to read the X-forwarded- related headers.
   *
   * @return a spring boot customizer that adds the ForwardedRequestCustomizer
   * to the jetty http connector.
   */
  private JettyServerCustomizer forwardedRequestCustomizer() {
    log.trace("Entering JettyServerCustomizer");
    JettyServerCustomizer result = new JettyServerCustomizer() {
      @Override
      public void customize(final Server server) {
        for (Connector connector : server.getConnectors()) {
          if (connector instanceof ServerConnector) {
            HttpConnectionFactory factory = ((ServerConnector) connector)
                .getConnectionFactory(HttpConnectionFactory.class);
            factory.getHttpConfiguration().addCustomizer(
                new ForwardedRequestCustomizer());
          }
        }
      }
    };
    log.trace("Leaving JettyServerCustomizer");
    return result;
  }

  /** Creates the home servlet that redirects to a configurable landing url.
   *
   * K2 maps this servlet to the web context root ("/") and redirects the user
   * to another page that usually belongs to some module.
   *
   * NOTE: this is not the best approach, because this servlet only knows about
   * a single url. We will, in the future, let application writers provide a
   * strategy to determine the landing page.
   *
   * @param landingUrl the application-wise landing url. Null if not defined.
   *
   * @return the home servlet (an instance of HomeServlet) mapped to the root
   * path of the web application context. If landingUrl is null this servlet
   * shows a hardcoded message.
   */
  @Bean(name = "k2.homeServlet")
  public ServletRegistrationBean homeServlet(
      @Value("${k2.landingUrl:}") final String landingUrl) {

    ServletRegistrationBean servletBean = null;
    if (landingUrl != null) {
      servletBean = new ServletRegistrationBean(new HomeServlet(landingUrl),
          false, "");
      servletBean.setOrder(Integer.MAX_VALUE);
    }
    return servletBean;
  }
}

