package com.nexblocks.authguard.saml.routes;

import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@DependsOnConfiguration("saml")
public class SamlSsoResourcesHandler implements VertxApiHandler {

    static {
        try { InitializationService.initialize(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SamlSsoResourcesHandler.class);

    private final Map<String, byte[]> cache = new HashMap<>();

    public void register(final Router router) {
        router.get("/saml/:domain/resources/saml-sso-script.js").handler(this::serveSsoScript);
        router.get("/saml/:domain/resources/tailwind.js").handler(this::serveTailwind);
        router.get("/saml/:domain/resources/preact.umd.js").handler(this::servePreact);
        router.get("/saml/:domain/resources/hooks.umd.js").handler(this::serverPreactHooks);
        router.get("/saml/:domain/resources/htm.umd.js").handler(this::serveHtm);
    }

    private void serveSsoScript(final RoutingContext context) {
        Optional<byte[]> bytesOpt = getResource("/saml-sso-script.js");

        if (bytesOpt.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }

        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
                .end(Buffer.buffer(bytesOpt.get()));
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

        try (InputStream stream = SamlSsoResourcesHandler.class.getResourceAsStream(resourcePath)) {
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