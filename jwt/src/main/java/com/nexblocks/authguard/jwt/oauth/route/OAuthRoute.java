package com.nexblocks.authguard.jwt.oauth.route;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.jwt.oauth.service.OAuthService;
import io.vavr.control.Either;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;

@DependsOnConfiguration("oauth")
public class OAuthRoute implements VertxApiHandler {
    private final OAuthService oAuthService;

    @Inject
    public OAuthRoute(final OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    public void register(final Router router) {
        router.get("/oauth/oidc/auth").handler(this::openIdConnectAuthFlows);
        router.post("/oauth/oidc/token").handler(this::openIdConnectAuthFlows);
        router.get("/oauth/auth_url")
                .handler(VertxRolesAccessHandler.adminOrAuthClient())
                .handler(this::getAuthUrl);
        router.post("/oauth/authorize")
                .handler(VertxRolesAccessHandler.adminOrAuthClient())
                .handler(this::authorize);
    }

    void openIdConnectAuthFlows(final RoutingContext context) {
        Either<RequestValidationError, ImmutableOpenIdConnectRequest> request
                = OpenIdConnectRequestParser.authRequestFromQueryParams(context, "code");

        if (request.isLeft()) {
            context.response().setStatusCode(400)
                    .end(Json.encode(request.getLeft()));
        } else {
            context.response().setStatusCode(501)
                    .end(Json.encode(new Error("501", "Feature not currently supported")));
        }
    }

    void openIdConnectToken(final RoutingContext context) {

    }

    void getAuthUrl(final RoutingContext context) {
        String provider = context.request().getFormAttribute("provider");

        if (provider == null) {
            context.response().setStatusCode(400)
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("provider", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
        } else {
            oAuthService.getAuthorizationUrl(provider)
                    .subscribe()
                    .with(context::redirect);
        }
    }

    void authorize(final RoutingContext context) {
        String provider = context.queryParam("provider").get(0);
        String state = context.queryParam("state").get(0);
        String code = context.queryParam("code").get(0);

        if (provider == null) {
            context.response().setStatusCode(400)
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("provider", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
            return;
        }

        if (state == null) {
            context.response().setStatusCode(400)
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("state", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
            return;
        }

        if (code == null) {
            context.response().setStatusCode(400)
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("code", ViolationType.MISSING_REQUIRED_VALUE)
                    ))));
            return;
        }

        context.json(oAuthService.exchangeAuthorizationCode(provider, state, code));
    }
}
