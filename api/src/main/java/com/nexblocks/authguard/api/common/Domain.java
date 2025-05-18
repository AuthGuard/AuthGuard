package com.nexblocks.authguard.api.common;

import io.javalin.http.Context;
import io.vertx.ext.web.RoutingContext;

public class Domain {
    public static String fromContext(final Context context) {
        return context.pathParam("domain");
    }

    public static String fromContext(final RoutingContext context) {
        return context.pathParam("domain");
    }
}
