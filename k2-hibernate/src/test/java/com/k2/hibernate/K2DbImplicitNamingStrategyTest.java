/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertThat;

import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;

public class K2DbImplicitNamingStrategyTest {

  private Identifier name;
  private K2DbImplicitNamingStrategy strategy;
  private ImplicitEntityNameSource source;

  @Before public void setUp() {

    name = mock(Identifier.class);

    ImplicitNamingStrategy delegate = mock(ImplicitNamingStrategy.class);
    when(delegate.determinePrimaryTableName(
        Mockito.isA(ImplicitEntityNameSource.class))).thenReturn(name);

    source = mock(ImplicitEntityNameSource.class);

    strategy = new K2DbImplicitNamingStrategy(delegate);
  }

  @Test public void determinePrimaryTableName_lowerCase() {
    when(name.getText()).thenReturn("ABC");

    Identifier i = strategy.determinePrimaryTableName(source);
    assertThat(i.getText(), is("abc"));
  }

  @Test public void determinePrimaryTableName_manyUnderscores() {
    when(name.getText()).thenReturn("a_____b");

    Identifier i = strategy.determinePrimaryTableName(source);
    assertThat(i.getText(), is("a_b"));
  }

  @Test public void determinePrimaryTableName_withNumbers() {
    when(name.getText()).thenReturn("a11");

    Identifier i = strategy.determinePrimaryTableName(source);
    assertThat(i.getText(), is("a_11"));
  }

  @Test public void determinePrimaryTableName_camelCase() {
    when(name.getText()).thenReturn("aSimpleName");

    Identifier i = strategy.determinePrimaryTableName(source);
    assertThat(i.getText(), is("a_simple_name"));
  }

  @Test public void determinePrimaryTableName_acronym() {
    when(name.getText()).thenReturn("imageURL");

    Identifier i = strategy.determinePrimaryTableName(source);
    assertThat(i.getText(), is("image_url"));
  }
}

