package com.nexblocks.authguard.api.routes;

import io.vertx.ext.web.Router;

public interface VertxApiHandler {
    void register(Router router);
}
