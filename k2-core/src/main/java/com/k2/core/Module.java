/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/** Marks this component as a k2 module.
 *
 * This is an optional annotation that lets module writers declare some
 * information about their modules, like the file system relative path
 * of the module resources.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Module {

  /** The default value for the short name.
   *
   * This default value indicates that the module writer did not specify
   * any short name, so ModuleDefinition.getModuleShortName will return null.
   */
  String NO_SHORT_NAME = "SHORT_NAME_NOT_SPECIFIED";

  /** Defines a short name for the module.
   *
   * Other modules may use this short name to different purposes, for example,
   * Hibernate module uses this as a prefix for the module tables.
   *
   * @return the short name for the module, it may be the constant
   * NO_SHORT_NAME when not specified. Never null.
   */
  String shortName() default NO_SHORT_NAME;

  /** Indicates the file system relative path of the module resources.
   *
   * This is used in debug mode to tell k2 where to find resources in
   * the file system to reload them when changed during development.
   *
   * @return a file system relative path, usually
   * ../[mvn module]/src/main/resources.
   */
  String relativePath() default "";
}

