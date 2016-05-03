#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package};

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

