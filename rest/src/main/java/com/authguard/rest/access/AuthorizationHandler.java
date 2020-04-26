package com.authguard.rest.access;

import com.authguard.service.ApiKeysService;
import com.authguard.service.exchange.helpers.BasicAuth;
import com.authguard.service.model.AppBO;
import com.google.inject.Inject;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.model.AccountBO;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AuthorizationHandler implements Handler {
    private final BasicAuth basicAuth;
    private final ApiKeysService apiKeysService;

    @Inject
    public AuthorizationHandler(final BasicAuth basicAuth, final ApiKeysService apiKeysService) {
        this.basicAuth = basicAuth;
        this.apiKeysService = apiKeysService;
    }

    @Override
    public void handle(@NotNull final Context context) {
        Optional.ofNullable(context.header("Authorization"))
                .map(this::parseAuthorization)
                .filter(parts -> parts.length == 2)
                .ifPresent(parts -> populateActor(context, parts));
    }

    private String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException("Invalid format for authorization value");
        }

        return parts;
    }

    private void populateActor(final Context context, final String[] authorization) {
        switch (authorization[0]) {
            case "Basic":
                populateBasicActor(context, authorization[1]);
                break;

            case "Bearer":
                populateBearerActor(context, authorization[1]);
                return;

            default:
                context.status(401).result("Unrecognized authorization scheme");
        }
    }

    private void populateBasicActor(final Context context, final String base64Credentials) {
        final Optional<AccountBO> actorAccount = basicAuth.authenticateAndGetAccount("Basic " + base64Credentials);

        actorAccount.ifPresentOrElse(account -> context.attribute("actor", account),
                () -> context.status(401).result(""));
    }

    private void populateBearerActor(final Context context, final String apiKey) {
        final Optional<AppBO> actorApp = apiKeysService.validateApiKey(apiKey);

        actorApp.ifPresentOrElse(app -> context.attribute("actor", app),
                () -> context.status(401).result(""));
    }
}
