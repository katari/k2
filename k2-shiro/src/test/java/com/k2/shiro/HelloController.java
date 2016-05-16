/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** A simple controller that just returns the string "Hello there".
 */
@Controller
public class HelloController {

  /** Mapped to theThe endpoint "/hi.html", responds the string "Hello there".
   *
   * @return a hardcoded string "Hello there", never null.
   */
  @RequestMapping(value = "/hi.html", method = RequestMethod.GET)
  public HttpEntity<String> hi() {
    return new HttpEntity<String>("Hello there");
  }
}

