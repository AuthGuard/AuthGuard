package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.dto.entities.AccountDTO;
import com.nexblocks.authguard.api.dto.entities.CredentialsDTO;
import com.nexblocks.authguard.api.dto.entities.PasswordResetTokenDTO;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.PasswordResetRequestDTO;
import com.nexblocks.authguard.api.dto.requests.PasswordResetTokenRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UserIdentifiersRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.CredentialsApi;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.common.RestJsonMapper;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.service.AccountCredentialsService;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.ClientBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CredentialsRoute extends CredentialsApi {
    private final RestMapper restMapper;
    private final AccountCredentialsService credentialsService;

    private final BodyHandler<UserIdentifiersRequestDTO> userIdentifiersRequestBodyHandler;
    private final BodyHandler<PasswordResetTokenRequestDTO> passwordResetTokenRequestBodyHandler;
    private final BodyHandler<PasswordResetRequestDTO> passwordResetRequestBodyHandler;

    @Inject
    public CredentialsRoute(final RestMapper restMapper, final AccountCredentialsService credentialsService) {
        this.restMapper = restMapper;
        this.credentialsService = credentialsService;

        this.userIdentifiersRequestBodyHandler = new BodyHandler.Builder<>(UserIdentifiersRequestDTO.class)
                .build();
        this.passwordResetTokenRequestBodyHandler = new BodyHandler.Builder<>(PasswordResetTokenRequestDTO.class)
                .build();
        this.passwordResetRequestBodyHandler = new BodyHandler.Builder<>(PasswordResetRequestDTO.class)
                .build();
    }

    public void updatePassword(final Context context) {
        CredentialsDTO credentials = RestJsonMapper.asClass(context.body(), CredentialsDTO.class);
        Validator<Long> credentialsId = context.pathParam("id", Long.class);

        if (!credentialsId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AccountDTO> result = credentialsService.updatePassword(credentialsId.get(),
                        credentials.getPlainPassword(), Domain.fromContext(context))
                .thenApply(restMapper::toDTO);

        context.json(result);
    }

    public void addIdentifiers(final Context context) {
        Validator<Long> credentialsId = context.pathParam("id", Long.class);

        if (!credentialsId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        UserIdentifiersRequestDTO request = userIdentifiersRequestBodyHandler.getValidated(context);

        if (request.getOldIdentifier() != null && request.getIdentifiers().size() != 1) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("identifier", ViolationType.EXCEEDS_LENGTH_BOUNDARIES)
            ));
        }

        List<UserIdentifierBO> identifiers = request.getIdentifiers().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        CompletableFuture<AccountBO> result;

        if (request.getOldIdentifier() != null) {
            result = credentialsService.replaceIdentifier(credentialsId.get(), request.getOldIdentifier(),
                    identifiers.get(0), Domain.fromContext(context));
        } else {
            result = credentialsService.addIdentifiers(credentialsId.get(), identifiers, Domain.fromContext(context));
        }

        context.json(result.thenApply(restMapper::toDTO));
    }

    public void removeIdentifiers(final Context context) {
        Validator<Long> credentialsId = context.pathParam("id", Long.class);

        if (!credentialsId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        UserIdentifiersRequestDTO request = userIdentifiersRequestBodyHandler.getValidated(context);
        List<String> identifiers = request.getIdentifiers().stream()
                .map(UserIdentifierDTO::getIdentifier)
                .collect(Collectors.toList());

        CompletableFuture<AccountDTO> result = credentialsService.removeIdentifiers(credentialsId.get(), identifiers,
                        Domain.fromContext(context))
                .thenApply(restMapper::toDTO);

        context.json(result);
    }

    @Override
    public void createResetToken(final Context context) {
        PasswordResetTokenRequestDTO request = passwordResetTokenRequestBodyHandler.getValidated(context);

        if (!ActorDomainVerifier.verifyActorDomain(context, request.getDomain())) {
            return;
        }

        ClientBO actor = context.attribute("actor");
        boolean isAuthClient = actor.getClientType() == Client.ClientType.AUTH;

        CompletableFuture<PasswordResetTokenDTO> result = credentialsService
                .generateResetToken(request.getIdentifier(), !isAuthClient, request.getDomain())
                .thenApply(restMapper::toDTO); // prevent an auth client from seeing the reset token

        context.json(result);
    }

    @Override
    public void resetPassword(final Context context) {
        PasswordResetRequestDTO request = passwordResetRequestBodyHandler.getValidated(context);

        if (request.getIdentifier() != null &&
                !ActorDomainVerifier.verifyActorDomain(context, request.getDomain())) {
            return;
        }

        CompletableFuture<AccountBO> result;

        if (request.isByToken()) {
            result = credentialsService.resetPasswordByToken(request.getResetToken(), request.getNewPassword(),
                    Domain.fromContext(context));
        } else {
            result = credentialsService.replacePassword(request.getIdentifier(), request.getOldPassword(),
                    request.getNewPassword(), request.getDomain());
        }

        context.json(result.thenApply(restMapper::toDTO));
    }
}