package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.ApiKeyDTO;
import com.nexblocks.authguard.api.dto.entities.AppDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ApplicationsApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestJsonMapper;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
        String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        CreateAppRequestDTO request = appRequestRequestBodyHandler.getValidated(context);

        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        CompletableFuture<AppDTO> created = applicationsService.create(restMapper.toBO(request), requestContext)
                .thenApply(restMapper::toDTO);

        context.status(201).json(created);
    }

    public void getById(final Context context) {
        Validator<Long> applicationId = context.pathParam("id", Long.class);

        if (!applicationId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.getById(applicationId.get())
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.json(application);
    }

    public void getByExternalId(final Context context) {
        Validator<Long> applicationId = context.pathParam("id", Long.class);

        if (!applicationId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.getByExternalId(applicationId.get())
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.json(application);
    }

    public void update(final Context context) {
        AppDTO app = RestJsonMapper.asClass(context.body(), AppDTO.class);

        CompletableFuture<AppDTO> application = applicationsService.update(restMapper.toBO(app))
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.json(application);
    }

    public void deleteById(final Context context) {
        Validator<Long> applicationId = context.pathParam("id", Long.class);

        if (!applicationId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.delete(applicationId.get())
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.json(application);
    }

    public void activate(final Context context) {
        Validator<Long> applicationId = context.pathParam("id", Long.class);

        if (!applicationId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.activate(applicationId.get())
                .thenApply(restMapper::toDTO);

        context.json(application);
    }

    public void deactivate(final Context context) {
        Validator<Long> applicationId = context.pathParam("id", Long.class);

        if (!applicationId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.deactivate(applicationId.get())
                .thenApply(restMapper::toDTO);

        context.json(application);
    }

    @Override
    public void getApiKeys(final Context context) {
        Validator<Long> applicationId = context.pathParam("id", Long.class);

        if (!applicationId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<List<ApiKeyDTO>> keys = apiKeysService.getByAppId(applicationId.get())
                .thenApply(list -> list
                        .stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.json(keys);
    }
}
