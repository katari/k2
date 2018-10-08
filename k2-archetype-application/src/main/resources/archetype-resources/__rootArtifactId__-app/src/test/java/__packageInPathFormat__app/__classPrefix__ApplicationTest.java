#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package}app;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import com.k2.core.Application;
import com.k2.core.K2Environment;

public class ${classPrefix}ApplicationTest {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(
      ${classPrefix}ApplicationTest.class);

  private Application application;

  private String baseUrl;

  @Before public void setUp() {
    log.trace("Entering setUp");
    application = new ${classPrefix}Application();
    application.run(new String[] {"--server.port=0"});

    K2Environment environment;
    environment = application.getBean("environment", K2Environment.class);
    String port = environment.getProperty("local.server.port");
    baseUrl = "http://localhost:" + port;

    log.trace("Leaving setUp");
  }

  @After public void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void helloController() throws Exception {

    Response response = RestAssured.given()
        .header("Content-Type", "application/json")
        .get(url("/${classPrefix.toLowerCase()}/hi.html"));

    assertThat(response.getStatusCode(), is(200));
    assertThat(response.getBody().asString(), is("Hello there"));
  }

  /** Builds a url to hit the application.
   *
   * @param relativeUrl the relative url. It cannot be null.
   *
   * @return a full url to query the application. It cannot be null.
   */
  private String url(final String relativeUrl) {
    return baseUrl + relativeUrl;
  }
}

