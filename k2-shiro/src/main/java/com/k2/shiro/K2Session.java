/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.mgt.ValidatingSession;

/** A shiro session that keeps its information in a cookie.
 */
public class K2Session implements ValidatingSession {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(K2Session.class);

  /** The cipher to use to encrypt session cookies, never null. */
  private K2Cipher cipher;

  /** The servlet request, never null.
   */
  private HttpServletRequest request;

  /** The servlet response, never null.
   */
  private HttpServletResponse response;

  /**The host that originated the request, never null.
   */
  private String host;

  /** The session attributes, never null.
   */
  private Map<Object, Object> attributes = new HashMap<>();

  /** Indicates that the session should be stopped.
   *
   * This is used in the save operation, to clean up the session cookie.
   */
  private boolean stopping = false;

  /** Constructor, creates a k2 session.
   *
   * @param theCipher the cipher to use to encrypt and decript session cookies.
   * It cannot be null.
   *
   * @param theHost the host that originated the request.
   *
   * @param theRequest the servlet request. It cannot be null.
   *
   * @param theResponse the servlet request. It cannot be null.
   */
  public K2Session(final K2Cipher theCipher, final String theHost,
      final HttpServletRequest theRequest,
      final HttpServletResponse theResponse) {

    cipher = theCipher;
    request = theRequest;
    response = theResponse;
    host = theHost;

    if (request.getCookies() != null) {
      for (Cookie cookie : this.request.getCookies()) {
        if (cookie.getName().equals("k2session")) {
          deserialize(cookie.getValue());
        }
      }
    }
  }

  /** Deserializes the session from a string representation that is recovered
   * fron a browser cookie.
   *
   * @param value the serialized representation of the session. It cannot be
   * null.
   */
  @SuppressWarnings("unchecked")
  private void deserialize(final String value) {
    log.trace("Entering toMap()");
    try (ObjectInputStream ois = new ObjectInputStream(
        new ByteArrayInputStream(cipher.decrypt(value)))) {
      attributes = (Map<Object, Object>) ois.readObject();
    } catch (ClassNotFoundException | IOException e) {
      log.debug("Could not deserialize session, ignored");
    }
    log.trace("Leaving toMap()");
  }

  /** Sends the string representation of this k2 session to the client as a
   * browser cookie.
   */
  void save() {
    log.trace("Entering save");

    String sessionValue = "";

    SimpleCookie cookie = new SimpleCookie("k2session");
    if (stopping) {
      cookie.setMaxAge(0);
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(attributes);
        oos.close();
        sessionValue = cipher.encrypt(baos.toByteArray());
      } catch (IOException e) {
        log.debug("Error saving session", e);
      }
    }

    cookie.setValue(sessionValue);
    cookie.saveTo(request, response);

    log.trace("Leaving save");
  }

  @Override
  public void setAttribute(final Object key, final Object value) {
    attributes.put(key, value);
  }

  @Override
  public Object removeAttribute(final Object key)
      throws InvalidSessionException {
    Object value = attributes.remove(key);
    return value;
  }

  /** This is not relevant, this implementation simply returns the java object
   * identifier.
   */
  @Override
  public Serializable getId() {
    return System.identityHashCode(this);
  }

  /** Not implemented yet. */
  @Override
  public Date getStartTimestamp() {
    return null;
  }

  /** Not implemented yet. */
  @Override
  public Date getLastAccessTime() {
    return null;
  }

  /** Not implemented yet. */
  @Override
  public long getTimeout() throws InvalidSessionException {
    return 0;
  }

  /** Not implemented yet. */
  @Override
  public void setTimeout(final long maxIdleTimeInMillis)
      throws InvalidSessionException {
  }

  @Override
  public String getHost() {
    return host;
  }

  /** Not implemented yet. */
  @Override
  public void touch() throws InvalidSessionException {
  }

  @Override
  public void stop() throws InvalidSessionException {
    attributes.clear();
    stopping = true;
  }

  @Override
  public Collection<Object> getAttributeKeys() throws InvalidSessionException {
    return attributes.keySet();
  }

  @Override
  public Object getAttribute(final Object key) throws InvalidSessionException {
    return attributes.get(key);
  }

  @Override
  public boolean isValid() {
    return !stopping;
  }

  @Override
  public void validate() throws InvalidSessionException {
    if (stopping) {
      throw new InvalidSessionException("Session was stopped");
    }
  }
}

