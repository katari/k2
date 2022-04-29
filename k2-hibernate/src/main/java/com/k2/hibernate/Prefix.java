/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/** Customizes the embedded column name prefix when using
 * K2DbImplicitNamingStrategyComponentPath.
 *
 * When applied to @Embedded attributes of @Entity classes, this annotation
 * customizes the column name prefix of the corresponding embeddable
 * attributes.
 *
 * For example, assume you have an @Entity with an embeddable attribute value1,
 * that has another attribute named value2. By default, or when using
 * @Prefix(value = "", skip = false), the
 * K2DbImplicitNamingStrategyComponentPath will create a column named
 * "value_1_value_2". When using @Prefix(skip = true), the same column will be
 * named "value_2" (without the value_1_ prefix). When using @Prefix("new"),
 * the column will be named "new_value_2".
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Prefix {

  /** The prefix to add the database column name, without the trailing _.
   *
   * This defaults to the empty string, meaning that it will use the default
   * prefix as determined by K2DbImplicitNamingStrategyComponentPath.
   *
   * @return a prefix string, "" to use the default.
   */
  String value() default "";

  /** Whether to skip the prefix.
   *
   * This defaults to false, meaning that it will use the default prefix as
   * determined by K2DbImplicitNamingStrategyComponentPath.
   *
   * When setting skip to true, value must be the empty string.
   *
   * @return true to skip the prefix, false to use a prefix.
   */
  boolean skip() default false;
}
