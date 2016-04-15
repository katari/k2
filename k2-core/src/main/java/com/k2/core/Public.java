/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.core;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/** Marks a spring bean as public, ie., the bean will be available to other k2
 * modules.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Public {
}

