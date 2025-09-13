package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ClientBO;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class VertxAuthorizationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(VertxAuthorizationHandler.class);
    private static final String API_KEY_TYPE = "default";

    private final BasicAuthProvider basicAuth;
    private final ApiKeysService apiKeysService;
    private Set<String> unprotectedPaths = Collections.emptySet();

    @Inject
    public VertxAuthorizationHandler(final BasicAuthProvider basicAuth, final ApiKeysService apiKeysService) {
        this.basicAuth = basicAuth;
        this.apiKeysService = apiKeysService;
    }

    public VertxAuthorizationHandler setUnprotectedPaths(final Set<String> unprotectedPaths) {
        this.unprotectedPaths = unprotectedPaths;
        return this;
    }

    public void handleAuthorization(RoutingContext ctx) {
        String[] pathParts = ctx.request().path().split("/");

        if (unprotectedPaths.contains(pathParts[1])) {
            ctx.next();
            return;
        }

        String header = ctx.request().getHeader("Authorization");
        if (header == null) {
            ctx.next();
            return;
        }

        String[] parts = header.split(" ");
        if (parts.length != 2) {
            respondUnauthorized(ctx, "Invalid authorization header");
            return;
        }

        switch (parts[0]) {
            case "Basic" -> populateBasicActor(ctx, parts[1]);
            case "Bearer" -> populateBearerActor(ctx, parts[1]);
            default -> respondUnauthorized(ctx, "Unrecognized authorization scheme");
        }
    }

    private void populateBasicActor(final RoutingContext ctx, final String base64Credentials) {
        try {
            basicAuth.authenticateAndGetAccount(base64Credentials)
                    .subscribe().with(account -> {
                        if (!isActorAllowedInDomain(ctx, account)) {
                            respondForbidden(ctx);
                            return;
                        }

                        ctx.put("actor", account);
                        LOG.info("Authenticated account {} with basic credentials", account.getId());

                        ctx.next();
                    }, e -> {
                        LOG.info("Failed to authenticate with basic credentials", e);
                        respondUnauthorized(ctx, "Failed to authenticate with basic scheme");
                    });
        } catch (Exception e) {
            LOG.info("Failed to authenticate with basic credentials", e);
            respondUnauthorized(ctx, "Failed to authenticate with basic scheme");
        }
    }

    private void populateBearerActor(final RoutingContext ctx, final String token) {
        try {
            apiKeysService.validateClientApiKey(token, API_KEY_TYPE)
                    .subscribe().with(client -> {
                        if (!isActorAllowedInDomain(ctx, client)) {
                            respondForbidden(ctx);
                            return;
                        }

                        ctx.put("actor", client);
                        LOG.info("Authenticated client {} with bearer token", client.getId());

                        ctx.next();
                    }, e -> {
                        LOG.info("Failed to authenticate with basic credentials", e);
                        respondUnauthorized(ctx, "Failed to authenticate with basic scheme");
                    });
        } catch (CompletionException e) {
            LOG.warn("Failed bearer token auth", e.getCause());
            respondUnauthorized(ctx, "Failed to authenticate with bearer scheme");
        } catch (Throwable e) {
            LOG.warn("Exception validating bearer token", e);
            respondUnauthorized(ctx, "Failed to authenticate with bearer scheme");
        }
    }

    public boolean isActorAllowedInDomain(RoutingContext ctx, AccountBO account) {
        String domain = ctx.pathParam("domain");

        if (domain == null) {
            return true;
        }

        return Objects.equals(account.getDomain(), domain);
    }

    public boolean isActorAllowedInDomain(RoutingContext ctx, ClientBO account) {
        String domain = ctx.pathParam("domain");

        if (domain == null) {
            return true;
        }

        return Objects.equals(account.getDomain(), domain);
    }

    public void domainAuthorization(RoutingContext ctx) {
        Object actor = ctx.get("actor");
        String domain = ctx.pathParam("domain");

        if (actor instanceof AccountBO account) {
            if (!Objects.equals(account.getDomain(), domain)) {
                respondForbidden(ctx);
                return;
            }
        } else if (actor instanceof ClientBO client) {
            if (!Objects.equals(client.getDomain(), domain)) {
                respondForbidden(ctx);
                return;
            }
        }

        ctx.next();
    }

    private void respondUnauthorized(RoutingContext ctx, String message) {
        ctx.response().setStatusCode(401)
                .putHeader("Content-Type", "application/json")
                .end(io.vertx.core.json.Json.encode(new Error("401", message)));
    }

    private void respondForbidden(RoutingContext ctx) {
        ctx.response().setStatusCode(403)
                .putHeader("Content-Type", "application/json")
                .end(io.vertx.core.json.Json.encode(new Error("403", "Domain is outside the scope of the actor")));
    }
}
