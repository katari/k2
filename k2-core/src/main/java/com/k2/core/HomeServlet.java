/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.Validate;
import org.springframework.web.servlet.view.RedirectView;

/** Servlet mapped to the root web application context to redirect to a
 * configurable url.
 *
 * K2 maps this servlet to the '/' path. This servlet just redirects to
 * the configured url.
 *
 * Note: this servlet is very naive. We should allow the application to
 * configure a more complex strategy to select the landing url.
 */
public class HomeServlet extends HttpServlet {

  /** The serial version id. */
  private static final long serialVersionUID = 1L;

  /** The configured landing url, never null.
   */
  private String landingUrl;

  /** Creates the home servlet.
   *
   * @param theLandingUrl the landing url. If empty, it shows a hardcoded
   * message stating that you should configure it. It cannot be null.
   */
  public HomeServlet(final String theLandingUrl) {
    Validate.notNull(theLandingUrl, "Call setLandingUrl in your application.");
    landingUrl = theLandingUrl;
  }

  /** {@inheritDoc}
   */
  @Override
  public void service(final HttpServletRequest request,
      final HttpServletResponse response)
          throws ServletException, IOException {
    RedirectView redirectView;
    try {
      if (landingUrl.isEmpty()) {
        response.getWriter().print("Set k2.landingUrl to point to your home");
      } else {
        redirectView = new RedirectView(landingUrl);
        redirectView.render(null, request, response);
      }
    } catch (IOException | ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException("Error redirecting to home page", e);
    }
  }
}

