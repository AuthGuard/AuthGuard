package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.emb.AutoSubscribers;
import com.nexblocks.authguard.rest.ServerRunner;
import com.nexblocks.authguard.rest.config.ServerConfig;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VertxAuthGuardServer {
    private final static Logger log = LoggerFactory.getLogger(ServerRunner.class);

    private final Injector injector;
    private final ServerConfig serverConfig;

    public VertxAuthGuardServer(final Injector injector, final ServerConfig serverConfig) {
        this.injector = injector;
        this.serverConfig = serverConfig;
    }

    public void start(final Vertx vertx) {
        // Step 2: Get the main router
        Router router = Router.router(vertx);

        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));
        router.route().handler(BodyHandler.create());
        router.route().failureHandler(new GlobalExceptionHandler());

        VertxAuthorizationHandler authorizationHandler = injector.getInstance(VertxAuthorizationHandler.class);

        router.route().handler(authorizationHandler::handleAuthorization);
//        router.route("/domains/:domain/*").handler(authorizationHandler::domainAuthorization);

        // Step 3: Global error handling
        router.errorHandler(500, ctx -> {
            Throwable failure = ctx.failure();
            ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new MainVerticle.ErrorResponse("Internal server error", failure.getMessage())));
        });

        router.errorHandler(404, ctx -> {
            ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new MainVerticle.ErrorResponse("Not Found", "The requested resource was not found")));
        });

        // initialize subscribers
        injector.getInstance(AutoSubscribers.class).subscribe();

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
                .listen(serverConfig.getPort());
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
