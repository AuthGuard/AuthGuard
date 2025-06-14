package com.nexblocks.authguard.rest.access;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletionException;

public class AuthorizationHandler implements Handler {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationHandler.class);

    private static final String API_KEY_TYPE = "default";

    private final BasicAuthProvider basicAuth;
    private final ApiKeysService apiKeysService;

    @Inject
    public AuthorizationHandler(final BasicAuthProvider basicAuth, final ApiKeysService apiKeysService) {
        this.basicAuth = basicAuth;
        this.apiKeysService = apiKeysService;
    }

    @Override
    public void handle(@NotNull final Context context) {
        Optional.ofNullable(context.header("Authorization"))
                .map(this::splitAuthorization)
                .ifPresent(parts -> {
                    if (parts.length == 2) {
                        populateActor(context, parts);
                    } else {
                        context.status(401).json(new Error("401", "Invalid authorization header"));
                    }
                });
    }

    private String[] splitAuthorization(final String authorization) {
        return authorization.split("\\s");
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
                context.status(401).json(new Error("401", "Unrecognized authorization scheme"));
        }
    }

    private void populateBasicActor(final Context context, final String base64Credentials) {
        try {
            AccountBO account = basicAuth.authenticateAndGetAccount(base64Credentials)
                    .subscribeAsCompletionStage()
                    .join();
            LOG.info("Authenticated actor {} with basic credentials", account.getId());
            context.attribute("actor", account);
        } catch (Exception e) {
            LOG.info("Failed to authenticate actor with basic credentials");
            context.status(401).json(new Error("401", "Failed to authenticate with basic scheme"));
        }
    }

    private void populateBearerActor(final Context context, final String apiKey) {
        try {
            ClientBO actorClient = apiKeysService.validateClientApiKey(apiKey, API_KEY_TYPE)
                    .subscribeAsCompletionStage()
                    .join();
            LOG.info("Authenticated actor {} with bearer token", actorClient.getId());
            context.attribute("actor", actorClient);
        } catch (CompletionException e) {
            LOG.warn("Failed to authenticate actor with bearer token", e.getCause());

            context.status(401).json(new Error("401", "Failed to authenticate with bearer scheme"));
        } catch (Throwable e) {
            LOG.warn("An error occurred while validating API key", e);

            context.status(401).json(new Error("401", "Failed to authenticate with bearer scheme"));
        }
    }
}
