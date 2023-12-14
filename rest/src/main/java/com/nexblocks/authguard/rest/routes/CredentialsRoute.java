package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.api.dto.entities.AccountDTO;
import com.nexblocks.authguard.api.dto.entities.CredentialsDTO;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.PasswordResetRequestDTO;
import com.nexblocks.authguard.api.dto.requests.PasswordResetTokenRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UserIdentifiersRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.CredentialsApi;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestJsonMapper;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.service.AccountCredentialsService;
import com.nexblocks.authguard.service.model.*;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        final CredentialsDTO credentials = RestJsonMapper.asClass(context.body(), CredentialsDTO.class);
        final Validator<Long> credentialsId = context.pathParam("id", Long.class);

        if (!credentialsId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final Optional<AccountDTO> updated = credentialsService.updatePassword(credentialsId.get(), credentials.getPlainPassword())
                .map(restMapper::toDTO);

        if (updated.isPresent()) {
            context.status(200).json(updated.get());
        } else {
            context.status(404);
        }
    }

    public void addIdentifiers(final Context context) {
        final Validator<Long> credentialsId = context.pathParam("id", Long.class);

        if (!credentialsId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final UserIdentifiersRequestDTO request = userIdentifiersRequestBodyHandler.getValidated(context);

        if (request.getOldIdentifier() != null && request.getIdentifiers().size() != 1) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("identifier", ViolationType.EXCEEDS_LENGTH_BOUNDARIES)
            ));
        }

        final List<UserIdentifierBO> identifiers = request.getIdentifiers().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        if (request.getOldIdentifier() != null) {
            credentialsService.replaceIdentifier(credentialsId.get(), request.getOldIdentifier(), identifiers.get(0))
                    .map(restMapper::toDTO)
                    .ifPresentOrElse(context::json, () -> context.status(404));
        } else {
            credentialsService.addIdentifiers(credentialsId.get(), identifiers)
                    .map(restMapper::toDTO)
                    .ifPresentOrElse(context::json, () -> context.status(404));
        }
    }

    public void removeIdentifiers(final Context context) {
        final Validator<Long> credentialsId = context.pathParam("id", Long.class);

        if (!credentialsId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final UserIdentifiersRequestDTO request = userIdentifiersRequestBodyHandler.getValidated(context);
        final List<String> identifiers = request.getIdentifiers().stream()
                .map(UserIdentifierDTO::getIdentifier)
                .collect(Collectors.toList());

        credentialsService.removeIdentifiers(credentialsId.get(), identifiers)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
    }

    @Override
    public void createResetToken(final Context context) {
        final PasswordResetTokenRequestDTO request = passwordResetTokenRequestBodyHandler.getValidated(context);

        if (!ActorDomainVerifier.verifyActorDomain(context, request.getDomain())) {
            return;
        }

        final ClientBO actor = context.attribute("actor");
        final boolean isAuthClient = actor.getClientType() == Client.ClientType.AUTH;

        final PasswordResetTokenBO token = credentialsService
                .generateResetToken(request.getIdentifier(), !isAuthClient, request.getDomain()); // prevent an auth client from seeing the reset token

        context.json(restMapper.toDTO(token));
    }

    @Override
    public void resetPassword(final Context context) {
        final PasswordResetRequestDTO request = passwordResetRequestBodyHandler.getValidated(context);

        if (request.getIdentifier() != null &&
                !ActorDomainVerifier.verifyActorDomain(context, request.getDomain())) {
            return;
        }

        final Optional<AccountBO> updated;

        if (request.isByToken()) {
            updated = credentialsService.resetPasswordByToken(request.getResetToken(), request.getNewPassword());
        } else {
            updated = credentialsService.replacePassword(request.getIdentifier(), request.getOldPassword(),
                    request.getNewPassword(), request.getDomain());
        }

        if (updated.isPresent()) {
            context.status(200).json(updated.get());
        } else {
            context.status(404);
        }
    }
}