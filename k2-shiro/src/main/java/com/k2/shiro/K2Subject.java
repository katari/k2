/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.SecurityManager;

import org.apache.shiro.web.subject.support.WebDelegatingSubject;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;

/** A shiro subject that obtains it login information from a browser cookie
 * generated session.
 *
 * This subject lets you support 'stateless' web applications with very little
 * state in its 'session'. It knows about http requests and responses, and can
 * read and write state to a cookie.
 *
 * See K2Session and K2SessionManager for more information.
 */
public class K2Subject extends WebDelegatingSubject {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(K2Subject.class);

  /** The current session, initialized from a request cookie. It is never null.
   */
  private K2Session currentSession;

  /** Constructor, creates a new K2Subject.
   *
   * @param principals the principals that identifies this subject. May be null
   * if the subject is not authenticated.
   *
   * @param authenticated indicates if the subject is authenticated.
   *
   * @param host the host that originated the request.
   *
   * @param session a session that should be bound to this subject. If null,
   * this operation creates a new session from the request cookie.
   *
   * @param sessionEnabled indicates if sessions are enabled. This
   * implementation needs this to be true.
   *
   * @param theRequest the servlet request. It cannot be null.
   *
   * @param theResponse the servlet request. It cannot be null.
   *
   * @param securityManager the configured shiro security manager. It cannot
   * be null.
   */
  public K2Subject(final PrincipalCollection principals,
      final boolean authenticated, final String host, final Session session,
      final boolean sessionEnabled, final ServletRequest theRequest,
      final ServletResponse theResponse,
      final SecurityManager securityManager) {

    super(principals, authenticated, host, session, sessionEnabled, theRequest,
        theResponse, securityManager);

    currentSession = (K2Session) session;

    PrincipalCollection sessionPrincipals = (PrincipalCollection)
        session.getAttribute(
            DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
    if (sessionPrincipals != null && super.principals == null) {
      // Copy principals from session.
      super.principals = sessionPrincipals;
    }

    if (authenticated) {
      session.setAttribute(
          DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, Boolean.TRUE);
      super.authenticated = true;
    } else {
      Object auth = session.getAttribute(
          DefaultSubjectContext.AUTHENTICATED_SESSION_KEY);
      if (auth != null && ((boolean) auth)) {
        super.authenticated = true;
      } else {
        super.authenticated = false;
      }
    }
  }

  /** We override this because the SaveSessionFilter needs a K2Session to
   * store it in a cookie.
   */
  @Override
  public K2Session getSession(final boolean create) {
    return currentSession;
  }

  /** Attempts login (as implemented in the superclass), and stores a the
   * principal in the session.
   */
  @Override
  public void login(final AuthenticationToken token)
      throws AuthenticationException {
    log.trace("Entering login");
    super.login(token);
    session.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY,
        Boolean.TRUE);
    log.trace("Leaving login");
  }
}

