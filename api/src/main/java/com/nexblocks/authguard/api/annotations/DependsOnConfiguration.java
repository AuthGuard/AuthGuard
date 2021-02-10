package com.nexblocks.authguard.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tie a route to a configuration. This will
 * prevent the route from being injected unless
 * that top-level configuration is defined.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DependsOnConfiguration {
    String value();
}
