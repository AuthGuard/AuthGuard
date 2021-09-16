package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.CredentialsDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.CreateCredentialsRequestDTO;
import com.nexblocks.authguard.api.dto.requests.PasswordResetRequestDTO;
import com.nexblocks.authguard.api.dto.requests.PasswordResetTokenRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UserIdentifiersRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.CredentialsApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestJsonMapper;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.CredentialsService;
import com.nexblocks.authguard.service.model.CredentialsBO;
import com.nexblocks.authguard.service.model.PasswordResetTokenBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CredentialsRoute extends CredentialsApi {
    private final RestMapper restMapper;
    private final CredentialsService credentialsService;

    private final BodyHandler<CreateCredentialsRequestDTO> credentialsRequestBodyHandler;
    private final BodyHandler<UserIdentifiersRequestDTO> userIdentifiersRequestBodyHandler;
    private final BodyHandler<PasswordResetTokenRequestDTO> passwordResetTokenRequestBodyHandler;
    private final BodyHandler<PasswordResetRequestDTO> passwordResetRequestBodyHandler;

    @Inject
    public CredentialsRoute(final RestMapper restMapper, final CredentialsService credentialsService) {
        this.restMapper = restMapper;
        this.credentialsService = credentialsService;

        this.credentialsRequestBodyHandler = new BodyHandler.Builder<>(CreateCredentialsRequestDTO.class)
                .build();
        this.userIdentifiersRequestBodyHandler = new BodyHandler.Builder<>(UserIdentifiersRequestDTO.class)
                .build();
        this.passwordResetTokenRequestBodyHandler = new BodyHandler.Builder<>(PasswordResetTokenRequestDTO.class)
                .build();
        this.passwordResetRequestBodyHandler = new BodyHandler.Builder<>(PasswordResetRequestDTO.class)
                .build();
    }

    public void create(final Context context) {
        final String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        final CreateCredentialsRequestDTO request = credentialsRequestBodyHandler.getValidated(context);

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        final Optional<CredentialsDTO> created = Optional.of(restMapper.toBO(request))
                .map(credentialsBO -> credentialsService.create(credentialsBO, requestContext))
                .map(restMapper::toDTO);

        if (created.isPresent()) {
            context.status(201).json(created.get());
        } else {
            context.status(400).json(new Error("400", "Failed to create credentials"));
        }
    }

    public void update(final Context context) {
        final CredentialsDTO credentials = RestJsonMapper.asClass(context.body(), CredentialsDTO.class);

        if (credentials.getPlainPassword() != null) {
            context.status(400).json(new Error("400", "Password cannot be updated using regular update"));
            return;
        }

        final String credentialsId = context.pathParam("id");

        final Optional<CredentialsDTO> updated = Optional.of(credentials.withId(credentialsId))
                .map(restMapper::toBO)
                .flatMap(credentialsService::update)
                .map(restMapper::toDTO);

        if (updated.isPresent()) {
            context.status(200).json(updated.get());
        } else {
            context.status(404);
        }
    }

    public void updatePassword(final Context context) {
        final CredentialsDTO credentials = RestJsonMapper.asClass(context.body(), CredentialsDTO.class);
        final String credentialsId = context.pathParam("id");

        final Optional<CredentialsDTO> updated = credentialsService.updatePassword(credentialsId, credentials.getPlainPassword())
                .map(restMapper::toDTO);

        if (updated.isPresent()) {
            context.status(200).json(updated.get());
        } else {
            context.status(404);
        }
    }

    public void addIdentifiers(final Context context) {
        final String credentialsId = context.pathParam("id");
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
            credentialsService.replaceIdentifier(credentialsId, request.getOldIdentifier(), identifiers.get(0))
                    .map(restMapper::toDTO)
                    .ifPresentOrElse(context::json, () -> context.status(404));
        } else {
            credentialsService.addIdentifiers(credentialsId, identifiers)
                    .map(restMapper::toDTO)
                    .ifPresentOrElse(context::json, () -> context.status(404));
        }
    }

    public void removeIdentifiers(final Context context) {
        final String credentialsId = context.pathParam("id");
        final UserIdentifiersRequestDTO request = userIdentifiersRequestBodyHandler.getValidated(context);
        final List<String> identifiers = request.getIdentifiers().stream()
                .map(UserIdentifierDTO::getIdentifier)
                .collect(Collectors.toList());

        credentialsService.removeIdentifiers(credentialsId, identifiers)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
    }

    public void getById(final Context context) {
        final Optional<CredentialsDTO> credentials = credentialsService.getById(context.pathParam("id"))
                .map(restMapper::toDTO);

        if (credentials.isPresent()) {
            context.json(credentials.get());
        } else {
            context.status(404);
        }
    }

    @Override
    public void getByIdentifier(final Context context) {
        final Optional<CredentialsDTO> credentials = credentialsService.getByUsername(context.pathParam("identifier"))
                .map(restMapper::toDTO);

        if (credentials.isPresent()) {
            context.json(credentials.get());
        } else {
            context.status(404);
        }
    }

    @Override
    public void identifierExists(final Context context) {
        final boolean exists = credentialsService.getByUsername(context.pathParam("identifier"))
                .isPresent();

        if (exists) {
            context.status(200);
        } else {
            context.status(404);
        }
    }

    public void removeById(final Context context) {
        final Optional<CredentialsDTO> credentials = credentialsService.delete(context.pathParam("id"))
                .map(restMapper::toDTO);

        if (credentials.isPresent()) {
            context.json(credentials.get());
        } else {
            context.status(404);
        }
    }

    @Override
    public void createResetToken(final Context context) {
        final PasswordResetTokenRequestDTO request = passwordResetTokenRequestBodyHandler.getValidated(context);

        final PasswordResetTokenBO token = credentialsService.generateResetToken(request.getIdentifier());

        context.json(restMapper.toDTO(token));
    }

    @Override
    public void resetPassword(final Context context) {
        final PasswordResetRequestDTO request = passwordResetRequestBodyHandler.getValidated(context);

        final Optional<CredentialsBO> updated = credentialsService.resetPassword(request.getResetToken(), request.getPlainPassword());

        if (updated.isPresent()) {
            context.status(200).json(updated.get());
        } else {
            context.status(404);
        }
    }
}