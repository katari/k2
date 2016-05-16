/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.apache.shiro.mgt.SecurityManager;

import org.apache.shiro.web.subject.WebSubjectContext;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;

/** A shiro subject factory that creates K2Subject instances.
 */
public class K2SubjectFactory extends DefaultWebSubjectFactory {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(K2SubjectFactory.class);

  /** Creates a new k2 subject if the caller provided a web context.
   *
   * @param context the subject context used to create the subject. It must be
   * a WebSubjectContext for this factory to create a K2Subject. Otherwise
   * it creates the default subject as defined in the super class.
   *
   * @return returns a new subject, never null.
   */
  @Override
  public Subject createSubject(final SubjectContext context) {
    log.trace("Entering createSubject()");

    Subject result;
    if (!(context instanceof WebSubjectContext)) {
      log.debug("Non-web context, returning default subject type");
      result = super.createSubject(context);
    } else {
      log.debug("Web context, creating a K2Subject");
      WebSubjectContext wsc = (WebSubjectContext) context;
      SecurityManager securityManager = wsc.resolveSecurityManager();
      Session session = wsc.resolveSession();
      boolean sessionEnabled = wsc.isSessionCreationEnabled();
      PrincipalCollection principals = wsc.resolvePrincipals();
      boolean authenticated = wsc.resolveAuthenticated();
      String host = wsc.resolveHost();
      ServletRequest request = wsc.resolveServletRequest();
      ServletResponse response = wsc.resolveServletResponse();

      result = new K2Subject(principals, authenticated, host, session,
          sessionEnabled, request, response, securityManager);
    }
    log.debug("Leaving createSubject()");
    return result;
  }
}

