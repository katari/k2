/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger;

import java.io.InputStream;
import java.io.IOException;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;

import java.util.List;
import java.util.Scanner;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.commons.lang3.Validate;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.k2.core.ModuleDefinition;

/** Exposes the swagger-ui client to show the documentation of every registered
 * swagger spec.
 *
 * In debug mode, this controller loads the index.html from the file system.
 * Otherwise it loads it from the classpath.
 */
@Controller
public class SwaggerController {

  /** The class logger. */
  private static Logger log = LoggerFactory.getLogger(SwaggerController.class);

  /** Whether the application is loaded in debug mode or not.
   *
   * Defaults to false.
   */
  private boolean debug = false;

  /** The module definition of this module, never null.
   */
  private ModuleDefinition moduleDefinition;

  /** The list of swagger registries that contains the url of each swagger
   * spec, never null. */
  private List<SwaggerRegistry> registries;

  /** Constructor, creates a SwaggerController.
   *
   * @param theModuleDefinition the module definition of this module. It cannot
   * be null.
   *
   * @param theRegistries the registries with the swagger specs. It cannot be
   * null.
   *
   * @param isDebug true if this controller is operating en debug mode.
   */
  public SwaggerController(final ModuleDefinition theModuleDefinition,
      final List<SwaggerRegistry> theRegistries, final boolean isDebug) {
    Validate.notNull(theRegistries, "The registries cannot be null.");
    Validate.notNull(theModuleDefinition, "The definition cannot be null.");
    moduleDefinition = theModuleDefinition;
    registries = theRegistries;
    debug = isDebug;
  }

  /** Serves the '/' path with the documentation of each of the swagger specs
   * registered in the swagger module.
   *
   * @return the full page with the spec documentation, never returns null.
   */
  @RequestMapping(value = "/*", method = RequestMethod.GET,
      produces="text/html;charset=UTF-8")
  public HttpEntity<String> swaggerUi() {

    String template = null;

    if (debug) {
      log.debug("In debug mode, trying to load index.html from file system.");
      String parent = moduleDefinition.getRelativePath();
      if (parent != null) {
        String child = getClass().getPackage().getName().replace(".", "/");
        Path file = Paths.get(parent, child, "index.html");
        log.debug("Checking {}", file.toString());
        if (Files.isRegularFile(file)) {
          try {
            byte[] encoded = Files.readAllBytes(file);
            template = new String(encoded, "UTF-8");
          } catch (IOException e) {
            throw new RuntimeException("Error reading " + file.toString(), e);
          }
        }
      }
    }

    if (template == null) {
      InputStream content = getClass().getResourceAsStream("index.html");
      try (Scanner scanner = new Scanner(content)) {
        scanner.useDelimiter("\\Z");
        template = scanner.next();
      }
    }

    String urls = "";
    for (SwaggerRegistry registry: registries) {
      String idl = registry.getIdl();
      String path = registry.getRequestorPath();
      // Only non-null idls.
      if (idl != null) {
        urls += "{name: \"" + path + "\", url:\"" + idl + "\"},\n";
      }
    }
    String html;
    if (!urls.isEmpty()) {
      html = template.replaceAll("@@urls@@", "[" + urls + "]");
    } else {
      html = "No idl found - better remove the swagger module.";
    }
    return new HttpEntity<String>(html);
  }
}

