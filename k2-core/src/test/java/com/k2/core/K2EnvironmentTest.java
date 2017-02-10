/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
// import org.junit.After;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.AbstractEnvironment;

import org.springframework.core.env.MapPropertySource;

public class K2EnvironmentTest {

  private K2Environment environment;

  @Before public void setUp() {

    AbstractEnvironment mock = new AbstractEnvironment() {
      @Override
      public MutablePropertySources getPropertySources() {
        MutablePropertySources ps = new MutablePropertySources();
        HashMap<String, Object> map = new HashMap<>();
        map.put("prefix1.prop1", "value1");
        map.put("prefix2.sub.prop2", "value2");
        ps.addLast(new MapPropertySource("test", map));
        return ps;
      }
    };

    environment = new K2Environment(mock);
  }

  @Test public void getProperties_keepPrefix() {
    assertThat(environment.getProperties("prefix1")
        .getProperty("prefix1.prop1"), is("value1"));

    assertThat(environment.getProperties("prefix2")
        .getProperty("prefix2.sub.prop2"), is("value2"));

    assertThat(environment.getProperties("prefix2.sub")
        .getProperty("prefix2.sub.prop2"), is("value2"));
  }

  @Test public void getProperties_removePrefix() {
    assertThat(environment.getProperties("prefix1", true)
        .getProperty("prop1"), is("value1"));

    assertThat(environment.getProperties("prefix2", true)
        .getProperty("sub.prop2"), is("value2"));

    assertThat(environment.getProperties("prefix2.sub", true)
        .getProperty("prop2"), is("value2"));
  }
}

