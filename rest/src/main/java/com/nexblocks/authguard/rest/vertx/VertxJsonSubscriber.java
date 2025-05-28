package com.nexblocks.authguard.rest.vertx;

import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class VertxJsonSubscriber<T> implements UniSubscriber<T> {
    private final RoutingContext context;
    private final int statusCode;

    public VertxJsonSubscriber(final RoutingContext context) {
        this.context = context;
        this.statusCode = 200;
    }

    public VertxJsonSubscriber(final RoutingContext context, final int statusCode) {
        this.context = context;
        this.statusCode = statusCode;
    }

    @Override
    public void onSubscribe(final UniSubscription uniSubscription) {
        uniSubscription.request(1);
    }

    @Override
    public void onItem(final T result) {
        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(result));
    }

    @Override
    public void onFailure(final Throwable throwable) {
        context.fail(throwable);
    }
}
