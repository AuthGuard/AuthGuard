package com.authguard.rest.routes;

import com.authguard.api.dto.entities.AppDTO;
import com.authguard.api.dto.requests.CreateAppRequestDTO;
import com.authguard.api.routes.ApplicationsApi;
import com.authguard.rest.mappers.RestJsonMapper;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.util.BodyHandler;
import com.authguard.rest.util.IdempotencyHeader;
import com.authguard.service.ApplicationsService;
import com.authguard.service.model.RequestContextBO;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.Optional;

public class ApplicationsRoute extends ApplicationsApi {
    private final ApplicationsService applicationsService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAppRequestDTO> appRequestRequestBodyHandler;

    @Inject
    public ApplicationsRoute(final ApplicationsService applicationsService, final RestMapper restMapper) {
        this.applicationsService = applicationsService;
        this.restMapper = restMapper;

        this.appRequestRequestBodyHandler = new BodyHandler.Builder<>(CreateAppRequestDTO.class)
                .build();
    }

    public void create(final Context context) {
        final String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        final CreateAppRequestDTO request = appRequestRequestBodyHandler.getValidated(context);

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        final Optional<Object> created = Optional.of(restMapper.toBO(request))
                .map(appBO -> applicationsService.create(appBO, requestContext))
                .map(restMapper::toDTO);

        if (created.isPresent()) {
            context.status(201).json(created.get());
        } else {
            context.status(400).result("Failed to create application");
        }
    }

    public void getById(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.getById(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    public void getByExternalId(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.getById(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    public void update(final Context context) {
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

    public void deleteById(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.delete(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    public void activate(final Context context) {
        final String applicationId = context.pathParam("id");

        final Optional<AppDTO> application = applicationsService.activate(applicationId)
                .map(restMapper::toDTO);

        if (application.isPresent()) {
            context.status(200).json(application.get());
        } else {
            context.status(404);
        }
    }

    public void deactivate(final Context context) {
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
