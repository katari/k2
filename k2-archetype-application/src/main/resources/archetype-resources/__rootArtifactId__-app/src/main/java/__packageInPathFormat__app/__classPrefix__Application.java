#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package}app;

import java.util.List;
import java.util.LinkedList;

import com.k2.core.Application;
import com.k2.core.Registrator;

import ${package}.${classPrefix};

/** The ${classPrefix}application.
 *
 * This application includes the ${classPrefix} module.
 */
public class ${classPrefix}Application extends Application {

  /** Constructor, creates a ${classPrefix}Application and registers the
   * ${classPrefix} module.
   */
  public ${classPrefix}Application() {
    super(new $classPrefix());
  }
}

