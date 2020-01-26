package com.authguard.rest.routes;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import com.authguard.rest.dto.CredentialsDTO;
import com.authguard.service.CredentialsService;

import java.util.Optional;

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
        post("/", this::create);
        put("/:id", this::update);
        put("/:id/password", this::updatePassword);
        get("/:id", this::getById);
        delete("/:id", this::removeById);
    }

    private void create(final Context context) {
        final CredentialsDTO credentials = context.bodyAsClass(CredentialsDTO.class);

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
        final CredentialsDTO credentials = context.bodyAsClass(CredentialsDTO.class);

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
        final CredentialsDTO credentials = context.bodyAsClass(CredentialsDTO.class);
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