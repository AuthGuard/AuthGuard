package com.authguard.rest.routes;

import com.authguard.rest.access.ActorRoles;
import com.authguard.rest.dto.*;
import com.authguard.service.CredentialsService;
import com.authguard.service.model.UserIdentifierBO;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class CredentialsRoute implements EndpointGroup {
    private final RestMapper restMapper;
    private final CredentialsService credentialsService;

    @Inject
    public CredentialsRoute(final RestMapper restMapper, final CredentialsService credentialsService) {
        this.restMapper = restMapper;
        this.credentialsService = credentialsService;
    }

    @Override
    public void addEndpoints() {
        get("/:id", this::getById, ActorRoles.adminClient());

        post("/", this::create, ActorRoles.of("authguard_admin_client", "one_time_admin"));

        put("/:id", this::update, ActorRoles.adminClient());
        patch("/:id/password", this::updatePassword, ActorRoles.adminClient());

        patch("/:id/identifiers", this::addIdentifiers, ActorRoles.adminClient());
        delete("/:id/identifiers", this::removeIdentifiers, ActorRoles.adminClient());

        delete("/:id", this::removeById, ActorRoles.adminClient());
    }

    private void create(final Context context) {
        final CredentialsDTO credentials = RestJsonMapper.asClass(context.body(), CredentialsDTO.class);

        final Optional<CredentialsDTO> created = Optional.of(credentials)
                .map(restMapper::toBO)
                .map(credentialsService::create)
                .map(restMapper::toDTO);

        if (created.isPresent()) {
            context.status(201).json(created.get());
        } else {
            context.status(400).result("Failed to create account");
        }

    }

    private void update(final Context context) {
        final CredentialsDTO credentials = RestJsonMapper.asClass(context.body(), CredentialsDTO.class);

        if (credentials.getPlainPassword() != null) {
            context.status(400).result("Password cannot be updated using regular update");
            return;
        }

        final String credentialsId = context.pathParam("id");

        final Optional<CredentialsDTO> updated = Optional.of(credentials.withId(credentialsId))
                .map(restMapper::toBO)
                .flatMap(credentialsService::update)
                .map(restMapper::toDTO);

        if (updated.isPresent()) {
            context.status(400).json(updated.get());
        } else {
            context.status(404);
        }
    }

    private void updatePassword(final Context context) {
        final CredentialsDTO credentials = RestJsonMapper.asClass(context.body(), CredentialsDTO.class);
        final String credentialsId = context.pathParam("id");

        final Optional<CredentialsDTO> updated = Optional.of(credentials.withId(credentialsId))
                .map(restMapper::toBO)
                .flatMap(credentialsService::updatePassword)
                .map(restMapper::toDTO);

        if (updated.isPresent()) {
            context.status(400).json(updated.get());
        } else {
            context.status(404);
        }
    }

    private void addIdentifiers(final Context context) {
        final String credentialsId = context.pathParam("id");
        final UserIdentifiersRequestDTO request = RestJsonMapper.asClass(context.body(), UserIdentifiersRequestDTO.class);
        final List<UserIdentifierBO> identifiers = request.getIdentifiers().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        credentialsService.addIdentifiers(credentialsId, identifiers)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
    }

    private void removeIdentifiers(final Context context) {
        final String credentialsId = context.pathParam("id");
        final UserIdentifiersRequestDTO request = RestJsonMapper.asClass(context.body(), UserIdentifiersRequestDTO.class);
        final List<String> identifiers = request.getIdentifiers().stream()
                .map(UserIdentifierDTO::getIdentifier)
                .collect(Collectors.toList());

        credentialsService.removeIdentifiers(credentialsId, identifiers)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
    }

    private void getById(final Context context) {
        final Optional<CredentialsDTO> credentials = credentialsService.getById(context.pathParam("id"))
                .map(restMapper::toDTO);

        if (credentials.isPresent()) {
            context.json(credentials.get());
        } else {
            context.status(404);
        }
    }

    private void removeById(final Context context) {
        final Optional<CredentialsDTO> credentials = credentialsService.delete(context.pathParam("id"))
                .map(restMapper::toDTO);

        if (credentials.isPresent()) {
            context.json(credentials.get());
        } else {
            context.status(404);
        }
    }
}