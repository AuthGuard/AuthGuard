package com.authguard.jwt.oauth;

import io.vertx.core.Vertx;

/**
 * A singleton holder of a {@link Vertx} instance.
 */
public class VertxContext {
    private static final Vertx vertx = Vertx.vertx();

    public static Vertx get() {
        return vertx;
    }
}
