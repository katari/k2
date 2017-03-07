/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.Validate;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/** Exposes the swagger-ui client to show the documentation of every registered
 * swagger spec.
 */
@Controller
public class SwaggerController {

  /** The list of swagger registries that contains the url of each swagger
   * spec, never null. */
  private List<SwaggerRegistry> registries;

  /** Constructor, creates a SwaggerController.
   *
   * @param theRegistries the registries with the swagger specs. It cannot be
   * null.
   */
  public SwaggerController(final List<SwaggerRegistry> theRegistries) {
    Validate.notNull(theRegistries, "The registries cannot be null.");
    registries = theRegistries;
  }

  /** Serves the '/' path with the documentation of each of the swagger specs
   * registered in the swagger module.
   *
   * @param index a number that indicates which spec to server, starting from
   * 0.
   *
   * @return the full page with the spec documentation, never returns null.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public HttpEntity<String> swaggerUi(
      @RequestParam(name = "spec", defaultValue = "0", required = false)
      final int index) {

    String template;
    InputStream content = getClass().getResourceAsStream("index.html");
    try (Scanner scanner = new Scanner(content)) {
      scanner.useDelimiter("\\Z");
      template = scanner.next();
    }

    String swagger = "\n";
    String options = "";
    int count = 0;
    for (SwaggerRegistry registry: registries) {
      String idl = registry.getIdl();
      String path = registry.getRequestorPath();
      if (idl == null) {
        // Skip null idls.
        continue;
      }
      options += "    <option ";
      if (count == index) {
        options += "selected ";
      }
      options += "value='" + count + "'>" + idl + "</option>\n";

      if (count == index) {
        swagger += "  <div id='swagger-ui-container-" + count + "'";
        swagger += " class='swagger-ui-wrap'></div>\n";
        swagger += "  <script type='text/javascript'>\n";
        swagger += "    createSwagger('" + idl + "', ";
        swagger += "'swagger-ui-container-" + count + "', '";
        swagger += path + "');\n";
        swagger += "  </script>\n";
      }
      ++count;
    }
    String html = template.replaceAll("@@options@@", options);
    html = html.replaceAll("@@content@@", swagger);
    return new HttpEntity<String>(html);
  }
}

