/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.Validate;

import org.apache.shiro.web.session.mgt.WebSessionKey;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.session.mgt.SessionManager;

/** A shiro session manager that obtains the session from a browser cookie.
 */
public class K2SessionManager implements SessionManager {

  /** The cipher to use to encrypt session cookies, never null. */
  private K2Cipher cipher;

  /** Constructor, creates a new session manager.
   *
   * @param theCipher the cipher to use to encrypt session cookies. It cannot
   * be null.
   */
  public K2SessionManager(final K2Cipher theCipher) {
    Validate.notNull(theCipher, "The cipher cannot be null.");
    cipher = theCipher;
  }

  /** This should never be called because sessions start implicitly with the
   * first request, ie: sessions are always initialized from a cookie.
   */
  @Override
  public Session start(final SessionContext context) {
    throw new RuntimeException("Should never start a session.");
  }

  /** Obtains the session from the request wrapped in key.
   */
  @Override
  public Session getSession(final SessionKey key) throws SessionException {
    WebSessionKey wk = ((WebSessionKey) key);
    return new K2Session(cipher, wk.getServletRequest().getRemoteHost(),
        (HttpServletRequest) wk.getServletRequest(),
        (HttpServletResponse) wk.getServletResponse());
  }
}

