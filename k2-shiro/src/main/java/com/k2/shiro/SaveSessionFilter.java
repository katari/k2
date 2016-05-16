/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Filter that k2 adds to the shiro filter chain that sends the session
 * cookie to the browser.
 *
 * This filter buffers the response so that cookies can be sent as late as
 * possible.
 */
public class SaveSessionFilter extends AdviceFilter {

  @Override
  protected void executeChain(final ServletRequest request,
      final ServletResponse response, final FilterChain chain)
          throws Exception {

    ContentCachingResponseWrapper wrappedResponse
        = new ContentCachingResponseWrapper((HttpServletResponse) response) {

      /** Sends the cookies before writing the content. */
      @Override
      protected void copyBodyToResponse(final boolean complete)
          throws IOException {
        if (request.getAttribute("k2.cookieWritten") == null) {
          ((K2Session) SecurityUtils.getSubject().getSession()).save();
          request.setAttribute("k2.cookieWritten", true);
        }
        super.copyBodyToResponse(complete);
      }
    };

    chain.doFilter(request, wrappedResponse);
    wrappedResponse.copyBodyToResponse();
  }
}
