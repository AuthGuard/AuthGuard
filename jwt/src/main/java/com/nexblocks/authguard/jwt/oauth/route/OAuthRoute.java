package com.nexblocks.authguard.jwt.oauth.route;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.ActorRoles;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ApiRoute;
import com.nexblocks.authguard.jwt.oauth.service.OAuthService;
import io.javalin.http.Context;
import io.vavr.control.Either;

import java.util.Collections;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

@DependsOnConfiguration("oauth")
public class OAuthRoute implements ApiRoute {
    private final OAuthService oAuthService;

    @Inject
    public OAuthRoute(final OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @Override
    public String getPath() {
        return "oauth";
    }

    @Override
    public void addEndpoints() {
        get("/oidc/auth", this::openIdConnectAuthFlows);
        post("/oidc/token", this::openIdConnectAuthFlows);
        get("/auth_url", this::getAuthUrl, ActorRoles.adminOrAuthClient());
        post("/authorize", this::authorize, ActorRoles.adminOrAuthClient());
    }

    void openIdConnectAuthFlows(final Context context) {
        final Either<RequestValidationError, ImmutableOpenIdConnectRequest> request
                = OpenIdConnectRequestParser.fromContext(context, "code");

        if (request.isLeft()) {
            context.status(400).json(request.getLeft());
        } else {
            context.json(501).json(new Error("501", "Feature not currently supported"));
        }
    }

    void openIdConnectToken(final Context context) {

    }

    void getAuthUrl(final Context context) {
        final String provider = context.queryParam("provider");

        if (provider == null) {
            context.status(400).json(new RequestValidationError(Collections.singletonList(
                    new Violation("provider", ViolationType.MISSING_REQUIRED_VALUE)
            )));
        } else {
            final String url = oAuthService.getAuthorizationUrl(provider).join();

            context.redirect(url);
        }
    }

    void authorize(final Context context) {
        final String provider = context.queryParam("provider");
        final String state = context.queryParam("state");
        final String code = context.queryParam("code");

        if (provider == null) {
            context.status(400).json(new RequestValidationError(Collections.singletonList(
                    new Violation("provider", ViolationType.MISSING_REQUIRED_VALUE)
            )));
            return;
        }

        if (state == null) {
            context.status(400).json(new RequestValidationError(Collections.singletonList(
                    new Violation("state", ViolationType.MISSING_REQUIRED_VALUE)
            )));
            return;
        }

        if (code == null) {
            context.status(400).json(new RequestValidationError(Collections.singletonList(
                    new Violation("code", ViolationType.MISSING_REQUIRED_VALUE)
            )));
            return;
        }

        context.json(oAuthService.exchangeAuthorizationCode(provider, state, code));
    }
}
