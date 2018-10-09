#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package}app;

import org.springframework.context.annotation.Configuration;

import com.k2.core.Application;
import com.k2.swagger.Swagger;

import ${package}.${classPrefix};

/** The ${classPrefix}application.
 *
 * This application includes the ${classPrefix} module.
 */
@Configuration
public class ${classPrefix}Application extends Application {

  /** Constructor, creates a ${classPrefix}Application and registers the
   * ${classPrefix} module.
   */
  public ${classPrefix}Application() {
    super(new Swagger(), new $classPrefix());
  }
}

