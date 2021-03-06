package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.ApiKeyDTO;
import com.nexblocks.authguard.api.dto.entities.AppDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.api.routes.ApplicationsApi;
import com.nexblocks.authguard.rest.mappers.RestJsonMapper;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationsRoute extends ApplicationsApi {
    private final ApplicationsService applicationsService;
    private final ApiKeysService apiKeysService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAppRequestDTO> appRequestRequestBodyHandler;

    @Inject
    public ApplicationsRoute(final ApplicationsService applicationsService,
                             final ApiKeysService apiKeysService,
                             final RestMapper restMapper) {
        this.applicationsService = applicationsService;
        this.apiKeysService = apiKeysService;
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
            context.status(400).json(new Error("400", "Failed to create application"));
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

    @Override
    public void getApiKeys(final Context context) {
        final String applicationId = context.pathParam("id");

        final List<ApiKeyDTO> keys = apiKeysService.getByAppId(applicationId)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.status(200).json(keys);
    }
}
