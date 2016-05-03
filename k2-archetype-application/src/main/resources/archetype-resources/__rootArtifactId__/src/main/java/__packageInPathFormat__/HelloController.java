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

/** A simple controller that just returns the string "Hello there".
 */
@Controller
public class HelloController {

  /** Mapped to theThe endpoint "/hi.html", responds the string "Hello there"
   *
   * @return a hardcoded string "Hello there", never null.
   */
  @RequestMapping(value = "/hi.html", method = RequestMethod.GET)
  public HttpEntity<String> hi() {
    return new HttpEntity<String>("Hello there");
  }
}

