package org.auther.api;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.CredentialsDTO;
import org.auther.service.CredentialsService;

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
        put("/", this::update);
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
        context.status(404);
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