package com.nexblocks.authguard.rest.vertx;

import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public class VertxOptJsonSubscriber<T> implements UniSubscriber<Optional<T>> {
    private final RoutingContext context;

    public VertxOptJsonSubscriber(final RoutingContext context) {
        this.context = context;
    }

    @Override
    public void onSubscribe(final UniSubscription uniSubscription) {
        uniSubscription.request(1);
    }

    @Override
    public void onItem(final Optional<T> result) {
        if (result.isEmpty()) {
            context.response().setStatusCode(404).end();
        } else {
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(result.get()));
        }    }

    @Override
    public void onFailure(final Throwable throwable) {
        context.fail(throwable);
    }
}
