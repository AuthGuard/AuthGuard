package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.AccountCredentialsService;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.ClientBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CredentialsHandler implements VertxApiHandler {
    private final RestMapper restMapper;
    private final AccountCredentialsService credentialsService;
    private final BodyHandler<UserIdentifiersRequestDTO> userIdentifiersRequestBodyHandler;
    private final BodyHandler<PasswordResetTokenRequestDTO> passwordResetTokenRequestBodyHandler;
    private final BodyHandler<PasswordResetRequestDTO> passwordResetRequestBodyHandler;

    @Inject
    public CredentialsHandler(final RestMapper restMapper, final AccountCredentialsService credentialsService) {
        this.restMapper = restMapper;
        this.credentialsService = credentialsService;
        this.userIdentifiersRequestBodyHandler = new BodyHandler.Builder<>(UserIdentifiersRequestDTO.class).build();
        this.passwordResetTokenRequestBodyHandler = new BodyHandler.Builder<>(PasswordResetTokenRequestDTO.class).build();
        this.passwordResetRequestBodyHandler = new BodyHandler.Builder<>(PasswordResetRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.patch("/domains/:domain/credentials/:id/password")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::updatePassword);
        router.patch("/domains/:domain/credentials/:id/identifiers")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::addIdentifiers);
        router.delete("/domains/:domain/credentials/:id/identifiers")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::removeIdentifiers);

        router.post("/domains/:domain/credentials/reset_token")
                .handler(VertxRolesAccessHandler.adminOrAuthClient())
                .handler(this::createResetToken);
        router.post("/domains/:domain/credentials/reset")
                .handler(VertxRolesAccessHandler.adminOrAuthClient())
                .handler(this::resetPassword);
    }

    private void updatePassword(final RoutingContext context) {
        try {
            long credentialsId = Long.parseLong(context.pathParam("id"));
            CredentialsDTO credentials = Json.decodeValue(context.body().asString(), CredentialsDTO.class);

            credentialsService.updatePassword(credentialsId, credentials.getPlainPassword(), context.pathParam("domain"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }

    private void addIdentifiers(final RoutingContext context) {
        try {
            long credentialsId = Long.parseLong(context.pathParam("id"));
            UserIdentifiersRequestDTO request = userIdentifiersRequestBodyHandler.getValidated(context);

            if (request.getOldIdentifier() != null && request.getIdentifiers().size() != 1) {
                throw new RequestValidationException(Collections.singletonList(
                        new Violation("identifier", ViolationType.EXCEEDS_LENGTH_BOUNDARIES)));
            }

            List<UserIdentifierBO> identifiers = request.getIdentifiers().stream()
                    .map(restMapper::toBO)
                    .collect(Collectors.toList());

            CompletableFuture<AccountBO> result;

            if (request.getOldIdentifier() != null) {
                result = credentialsService.replaceIdentifier(credentialsId, request.getOldIdentifier(),
                        identifiers.get(0), context.pathParam("domain"));
            } else {
                result = credentialsService.addIdentifiers(credentialsId, identifiers, context.pathParam("domain"));
            }

            result.thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }

    private void removeIdentifiers(final RoutingContext context) {
        try {
            long credentialsId = Long.parseLong(context.pathParam("id"));
            UserIdentifiersRequestDTO request = userIdentifiersRequestBodyHandler.getValidated(context);
            List<String> identifiers = request.getIdentifiers().stream()
                    .map(UserIdentifierDTO::getIdentifier)
                    .collect(Collectors.toList());

            credentialsService.removeIdentifiers(credentialsId, identifiers, context.pathParam("domain"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }

    private void createResetToken(final RoutingContext context) {
        try {
            PasswordResetTokenRequestDTO request = passwordResetTokenRequestBodyHandler.getValidated(context);

            if (!ActorDomainVerifier.verifyActorDomain(context, request.getDomain())) {
                context.response().setStatusCode(403).end();
                return;
            }

            ClientBO actor = context.get("actor");
            boolean isAuthClient = actor.getClientType() == Client.ClientType.AUTH;

            credentialsService.generateResetToken(request.getIdentifier(), !isAuthClient, request.getDomain())
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void resetPassword(final RoutingContext context) {
        try {
            PasswordResetRequestDTO request = passwordResetRequestBodyHandler.getValidated(context);

            if (request.getIdentifier() != null && !ActorDomainVerifier.verifyActorDomain(context, request.getDomain())) {
                context.response().setStatusCode(403).end();
                return;
            }

            CompletableFuture<AccountBO> result;

            if (request.isByToken()) {
                result = credentialsService.resetPasswordByToken(request.getResetToken(), request.getNewPassword(), request.getDomain());
            } else {
                result = credentialsService.replacePassword(request.getIdentifier(), request.getOldPassword(), request.getNewPassword(), request.getDomain());
            }

            result.thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }
}

