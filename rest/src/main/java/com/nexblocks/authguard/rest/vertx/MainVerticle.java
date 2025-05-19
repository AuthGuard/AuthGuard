package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.ServerRunner;
import com.nexblocks.authguard.rest.config.ServerConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MainVerticle extends AbstractVerticle {
    private final static Logger log = LoggerFactory.getLogger(ServerRunner.class);

    private final Injector injector;
    private final ServerConfig serverConfig;

    public MainVerticle(final Injector injector, final ServerConfig serverConfig) {
        this.injector = injector;
        this.serverConfig = serverConfig;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        // Step 2: Get the main router
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Step 3: Global error handling
        router.errorHandler(500, ctx -> {
            Throwable failure = ctx.failure();
            ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new ErrorResponse("Internal server error", failure.getMessage())));
        });

        router.errorHandler(404, ctx -> {
            ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new ErrorResponse("Not Found", "The requested resource was not found")));
        });

        // Step 4: Register routes
        List<Binding<VertxApiHandler>> routeBindings =
                injector.findBindingsByType(TypeLiteral.get(VertxApiHandler.class));

        routeBindings.stream().forEach(binding -> {
            VertxApiHandler route = binding.getProvider().get();

            log.info("Binding route {}", route);

            route.register(router);
        });

        // Step 5: Start server
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onSuccess(server -> {
                    System.out.println("Server started on port " + server.actualPort());
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    // Simple JSON error structure
    public static class ErrorResponse {
        public final String error;
        public final String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
