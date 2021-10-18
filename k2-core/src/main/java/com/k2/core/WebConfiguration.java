/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server
    .ConfigurableServletWebServerFactory;

import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty
    .JettyServletWebServerFactory;

import org.springframework.context.annotation.Bean;

/** The k2 web configuration that contains the spring beans to configure jetty
 * as the embedded web application.
 */
public class WebConfiguration {

  /** Configures the embedded servlet container implementation.
   *
   * This bean creates and configures a jetty embedded server. The server will
   * stop and throw an error if the spring application context cannot be
   * correctly initialized.
   *
   * @param port the port that jetty will listen on.
   *
   * @param contextPath the jetty context path. Defaults to the root context
   * path if not specified.
   *
   * @param serverCustomizer a the server customizer that configures specific
   * options in jetty. See serverCustomizer(...). It cannot be null.
   *
   * @return the factory, never null.
   */
  @Bean public ConfigurableServletWebServerFactory servletContainer(
      @Value("${server.port:8081}") final int port,
      @Value("${server.contextPath:}") final String contextPath,
      final JettyServerCustomizer serverCustomizer) {

    JettyServletWebServerFactory factory;
    factory = new JettyServletWebServerFactory("", port);
    factory.setContextPath(contextPath);
    factory.addServerCustomizers(serverCustomizer);
    factory.addConfigurations(new AbstractConfiguration() {
      @Override
      public void configure(final WebAppContext context) throws Exception {
        context.setThrowUnavailableOnStartupException(true);
      }
    });
    factory.setUseForwardHeaders(true);

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
  public ServletRegistrationBean<HomeServlet> homeServlet(
      @Qualifier("k2.landingUrl") final String landingUrl) {

    HomeServlet homeServlet = new HomeServlet(landingUrl);
    ServletRegistrationBean<HomeServlet> servletBean;
    servletBean = new ServletRegistrationBean<>(homeServlet, false, "");
    servletBean.setOrder(Integer.MAX_VALUE);
    return servletBean;
  }
}

