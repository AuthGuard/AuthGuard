package com.nexblocks.authguard.jwt.oauth;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.jwt.oauth.route.ImmutableOpenIdConnectRequest;
import com.nexblocks.authguard.jwt.oauth.route.OpenIdConnectRequestParser;
import com.nexblocks.authguard.jwt.oauth.service.OAuthService;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vavr.control.Either;

import java.util.Collections;

@DependsOnConfiguration("oauth")
public class OAuthApiHandler implements VertxApiHandler {
    private final OAuthService oAuthService;

    @Inject
    public OAuthApiHandler(final OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    public void register(final Router router) {
        router.get("/oauth/oidc/auth")
                .handler(this::openIdConnectAuthFlows);

        router.post("/oauth/oidc/token")
                .handler(this::openIdConnectAuthFlows); // placeholder, not implemented

        router.get("/oauth/auth_url")
                .handler(VertxRolesAccessHandler.adminOrAuthClient())
                .handler(this::getAuthUrl);

        router.post("/oauth/authorize")
                .handler(VertxRolesAccessHandler.adminOrAuthClient())
                .handler(this::authorize);
    }

    private void openIdConnectAuthFlows(final RoutingContext context) {
        Either<RequestValidationError, ImmutableOpenIdConnectRequest> request =
                OpenIdConnectRequestParser.authRequestFromQueryParams(context, "code");

        if (request.isLeft()) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(request.getLeft()));
        } else {
            context.response().setStatusCode(501)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new Error("501", "Feature not currently supported")));
        }
    }

    private void getAuthUrl(final RoutingContext context) {
        String provider = context.queryParam("provider").stream().findFirst().orElse(null);

        if (provider == null) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("provider", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
        } else {
            String url = oAuthService.getAuthorizationUrl(provider).subscribeAsCompletionStage().join();
            context.response().setStatusCode(302).putHeader("Location", url).end();
        }
    }

    private void authorize(final RoutingContext context) {
        String provider = context.queryParam("provider").stream().findFirst().orElse(null);
        String state = context.queryParam("state").stream().findFirst().orElse(null);
        String code = context.queryParam("code").stream().findFirst().orElse(null);

        if (provider == null) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("provider", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
            return;
        }

        if (state == null) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("state", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
            return;
        }

        if (code == null) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("code", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
            return;
        }

        context.response().putHeader("Content-Type", "application/json")
                .end(Json.encode(oAuthService.exchangeAuthorizationCode(provider, state, code)));
    }
}

