package com.k2.shiro;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.Validate;

import org.apache.shiro.codec.Base64;

/** A cipher that encrypts and decrypts information.
 *
 * This cipher encrypts an array of bytes an generates a base 64 encoded string
 * so that the encrypted data can be passed to browsers in cookies.
 */
public class K2Cipher {

  /** The encryption algorithm.*/
  private static final String ENCRYPTION_ALGORITHM = "RC4";

  /** The key to use to encrypt or decrypt messages.
   *
   * This is generated from the password provided in the constructor. It is
   * never null.
   */
  private byte[] key;

  /** Constructor, creates a k2 cipher.
   *
   * @param password the password to use. It cannot be null.
   */
  public K2Cipher(final String password) {
    Validate.notNull(password, "The password cannot be null.");
    MessageDigest sha;
    try {
      sha = MessageDigest.getInstance("SHA-1");
      key = sha.digest(password.getBytes("UTF-8"));
    } catch (Exception e) {
      throw new RuntimeException("Error creating key from password", e);
    }
  }

  /** Encrypts the provided plain text and generates an encrypted string.
   *
   * @param plainText the data to encrypt. It cannot be null.
   *
   * @return a string with the encrypted data, represented in base64. Never
   * returns null.
   */
  public String encrypt(final byte[] plainText) {
    try {
      SecretKeySpec keySpec = new SecretKeySpec(key, ENCRYPTION_ALGORITHM);
      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec);
      return Base64.encodeToString(cipher.doFinal(plainText));
    } catch (Exception e) {
      throw new RuntimeException("Error encrypting message.", e);
    }
  }

  /** Decrypts the given string and generates a plain array of bytes.
   *
   * @param cipherText the string to decrypt, as returned by encrypt. It cannot
   * be null.
   *
   * @return the decrypted plain array of bytes. Never returns null.
   */
  public byte[] decrypt(final String cipherText) {
    try {
      SecretKeySpec keySpec = new SecretKeySpec(key, ENCRYPTION_ALGORITHM);
      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec);
      return cipher.doFinal(Base64.decode(cipherText));
    } catch (Exception e) {
      throw new RuntimeException("Error decrypting message.", e);
    }
  }
}

