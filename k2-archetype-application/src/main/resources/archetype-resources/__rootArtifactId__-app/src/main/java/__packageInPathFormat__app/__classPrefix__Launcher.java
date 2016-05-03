#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package}app;

/** The sample application entry point.
 */
public class ${classPrefix}Launcher {

  /** Launches the sample application.
   *
   * @param args the command line arguments.
   */
  public static void main(final String[] args) {
    new ${classPrefix}Application().run(args);
  }
}

