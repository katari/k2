#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package};

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/** The ${classPrefix} module.
 */
@Component("${classPrefix.toLowerCase()}")
public class ${classPrefix} {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(${classPrefix}.class);

  /** A simple controller registered as a bean.
   *
   * @return the controller, never null.
   */
  @Bean public HelloController helloController() {
    return new HelloController();
  }
}

