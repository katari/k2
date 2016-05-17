/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import org.junit.Test;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class K2CipherTest {

  @Test public void cipher() {

    String sample = "plain text";
    K2Cipher cipher = new K2Cipher("password");
    String cipherText = cipher.encrypt(sample.getBytes());
    String plainText = new String(cipher.decrypt(cipherText));

    assertThat(plainText, is("plain text"));
  }

  @Test public void cipher_wrongPassword() {

    String sample = "plain text";
    K2Cipher cipher = new K2Cipher("password");
    String cipherText = cipher.encrypt(sample.getBytes());

    cipher = new K2Cipher("pass2");
    String plainText = new String(cipher.decrypt(cipherText));

    assertThat(plainText, not(is("plain text")));
  }
}

