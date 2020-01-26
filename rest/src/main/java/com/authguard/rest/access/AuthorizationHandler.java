package com.authguard.rest.access;

import com.google.inject.Inject;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import com.authguard.service.AuthenticationService;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.model.AccountBO;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Optional;

public class AuthorizationHandler implements Handler {
    private final AuthenticationService authenticationService;

    @Inject
    public AuthorizationHandler(final AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
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
                context.status(401).result("Bearer actors are currently not supported");
                return;

            default:
                context.status(401).result("Unrecognized authorization scheme");
        }
    }

    private void populateBasicActor(final Context context, final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        final Optional<AccountBO> actorAccount = authenticationService.authenticate(decoded[0], decoded[1]);

        actorAccount.ifPresentOrElse(account -> context.attribute("actor", account),
                () -> context.status(401).result(""));
    }
}
