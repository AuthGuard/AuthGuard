package com.nexblocks.authguard.jwt.oauth.route;

import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@DependsOnConfiguration("oauthSso")
public class OAuthSsoResourcesHandler implements VertxApiHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthSsoResourcesHandler.class);

    private final Map<String, byte[]> cache = new HashMap<>();

    public void register(final Router router) {
        router.get("/oidc/:domain/resources/tailwind.js").handler(this::serveTailwind);
        router.get("/oidc/:domain/resources/preact.umd.js").handler(this::servePreact);
        router.get("/oidc/:domain/resources/hooks.umd.js").handler(this::serverPreactHooks);
        router.get("/oidc/:domain/resources/htm.umd.js").handler(this::serveHtm);
    }

    private void serveTailwind(final RoutingContext context) {
        Optional<byte[]> bytesOpt = getResource("/tailwind.js");

        if (bytesOpt.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }

        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
                .end(Buffer.buffer(bytesOpt.get()));
    }

    private void servePreact(final RoutingContext context) {
        Optional<byte[]> bytesOpt = getResource("/preact.umd.js");

        if (bytesOpt.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }

        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
                .end(Buffer.buffer(bytesOpt.get()));
    }

    private void serverPreactHooks(final RoutingContext context) {
        Optional<byte[]> bytesOpt = getResource("/hooks.umd.js");

        if (bytesOpt.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }

        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
                .end(Buffer.buffer(bytesOpt.get()));
    }

    private void serveHtm(final RoutingContext context) {
        Optional<byte[]> bytesOpt = getResource("/htm.umd.js");

        if (bytesOpt.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }

        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
                .end(Buffer.buffer(bytesOpt.get()));
    }

    private Optional<byte[]> getResource(String resourcePath) {
        if (cache.containsKey(resourcePath)) {
            return Optional.of(cache.get(resourcePath));
        }

        try (InputStream stream = OAuthSsoResourcesHandler.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOG.error("Resource not found {}", resourcePath);
                return Optional.empty();
            }

            byte[] bytes = stream.readAllBytes();

            cache.put(resourcePath, bytes);
            return Optional.of(bytes);
        } catch (Exception e) {
            LOG.error("Failed to load resource {}", resourcePath, e);
            return Optional.empty();
        }
    }
}
