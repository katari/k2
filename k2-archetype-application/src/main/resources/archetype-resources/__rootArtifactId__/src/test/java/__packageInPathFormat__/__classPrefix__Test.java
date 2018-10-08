#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package};

import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import static org.junit.Assert.assertThat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

public class ${classPrefix}Test {

  /** The class logger. */
  private static Logger log = LoggerFactory.getLogger(${classPrefix}Test.class);

  private TestApplication application;

  @Before public void setUp() {
    log.trace("Entering setUp");
    application = TestApplication.start();

    log.trace("Leaving setUp");
  }

  @Test
  public void start() {
    assertThat(application, not(nullValue()));
    Assert.assertNotNull(application);
  }

  /** Tests that the /${classPrefix.toLowerCase()}/hello.html endpoint is alive
   * and returns the correct string.
   */
  @Test public void helloController() {

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
    return application.getBaseUrl() + relativeUrl;
  }
}

