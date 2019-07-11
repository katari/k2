/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;

import java.util.List;
import java.util.Scanner;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.view.RedirectView;

import com.k2.core.ModuleDefinition;

/** Exposes the swagger-ui client to show the documentation of every registered
 * swagger spec.
 *
 * In debug mode, this controller loads the index.html from the file system.
 * Otherwise it loads it from the classpath.
 *
 * This uses index.html, that relies on the swagger endpoint to not end in '/'.
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

  /** Redirects /swagger/ to /swagger, removing the trailing '/'.
   *
   * @return a redirect view to /swagger/, never null.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public RedirectView redirect() {
    return new RedirectView("/" + moduleDefinition.getModuleName(), true);
  }

  /** Serves the documentation of each of the swagger specs registered in the
   * swagger module.
   *
   * I could not find a way to configure the path mapping to just be /swagger,
   * so I mapped "/{anything}" and check that pathInfo is null.
   *
   * @param request the servlet request, never null.
   *
   * @return the full page with the spec documentation, never returns null.
   *
   * @throws NoHandlerFoundException when the url is not /swagger.
   */
  @RequestMapping(value = "/*", method = RequestMethod.GET,
      produces="text/html;charset=UTF-8")
  public HttpEntity<String> swaggerUi(
      final HttpServletRequest request) throws NoHandlerFoundException {

    // pathInfo is null for /swagger.
    if (request.getPathInfo() != null) {
      throw new NoHandlerFoundException(request.getMethod(),
          request.getRequestURI().toString(), null);
    }

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

      String path = registry.getRequestorPath();

      boolean useFileName = (registry.getIdls().size() > 1);

      for (String idl: registry.getIdls()) {
        urls += "{name: \"" + name(path, idl, useFileName)
          + "\", url:\"" + idl + "\"},\n";
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

  /** Determines a name for a swagger yaml spec.
   *
   * This operation builds the name based on the module name and the base yaml
   * file name. It is used to dissambiguate a yaml name in cases that a module
   * registers more than one yaml.
   *
   * @param moduleName the module name. It cannot be null.
   *
   * @param fileName the yaml file name. It cannot be null.
   *
   * @param useFileName whether to use the file name as part of the resulting
   * name. The caller should set this to true if the module registered more
   * than one yaml file.
   *
   * @return a name to use as a key to the yaml file in the swagger ui.
   */
  private String name(final String moduleName, final String fileName,
      final boolean useFileName) {
    String result = moduleName;

    if (useFileName) {
      File file = new File(fileName);
      String baseFileName = file.getName();
      int pos = baseFileName.lastIndexOf(".");
      if (pos > 0 && pos < (baseFileName.length() - 1)) {
        baseFileName = baseFileName.substring(0, pos);
      }
      result = moduleName + "/" + baseFileName;
    }
    return result;
  }
}

