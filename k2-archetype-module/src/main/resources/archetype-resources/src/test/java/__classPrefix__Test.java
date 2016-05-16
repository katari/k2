#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package};

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import org.springframework.context.annotation.Configuration;

import com.k2.core.Application;

/** The ${classPrefix} module integration tests.
 */
public class ${classPrefix}Test {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(${classPrefix}Test.class);

  private Application application;

  @Before public void setUp() {
    log.trace("Entering setUp");
    application = new TestApplication();
    application.run(new String[0]);
    log.trace("Leaving setUp");
  }

  @After public void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void getModuleBean() throws Exception {
    assertThat(application.getBean(${classPrefix}.class,
          "${classPrefix.toLowerCase()}"), is(not(nullValue())));
  }

  /////////////////////////////////////////////////////////////////////
  ///////////    The test application   ///////////////////////////////
  /////////////////////////////////////////////////////////////////////
  @Configuration
  public static class TestApplication extends Application {
    public TestApplication() {
      super(new ${classPrefix}());
    }

    public static void main(final String ... args) {
      Application application = new TestApplication();
      application.run(new String[0]);
    }
  }
}

