package com.authguard.rest.routes;

import com.authguard.api.dto.AppDTO;
import com.authguard.rest.access.ActorRoles;
import com.authguard.service.ApplicationsService;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ApplicationsRoute implements EndpointGroup {
    private final ApplicationsService applicationsService;
    private final RestMapper restMapper;

    @Inject
    public ApplicationsRoute(final ApplicationsService applicationsService, final RestMapper restMapper) {
        this.applicationsService = applicationsService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/", this::create, ActorRoles.anyAdmin());
        get("/:id", this::getById, ActorRoles.adminClient());
        get("/externalId/:id", this::getByExternalId, ActorRoles.adminClient());
        put("/:id", this::update, ActorRoles.adminClient());
        delete("/:id", this::deleteById, ActorRoles.adminClient());
        patch("/:id/activate", this::activate, ActorRoles.adminClient());
        patch("/:id/deactivate", this::deactivate, ActorRoles.adminClient());
    }

    private void create(final Context context) {
        final AppDTO app = RestJsonMapper.asClass(context.body(), AppDTO.class);

        final Optional<Object> created = Optional.of(app)
                .map(restMapper::toBO)
                .map(applicationsService::create)
                .map(restMapper::toDTO);

        if (created.isPresent()) {
            context.status(201).json(created.get());
        } else {
            context.status(400).result("Failed to create application");
        }
    }

    private void getById(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.getById(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    private void getByExternalId(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.getById(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    private void update(final Context context) {
        final AppDTO app = RestJsonMapper.asClass(context.body(), AppDTO.class);

        final Optional<AppDTO> application = Optional.of(app)
                .map(restMapper::toBO)
                .flatMap(applicationsService::update)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    private void deleteById(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.delete(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    private void activate(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.activate(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    private void deactivate(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.deactivate(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }
}
