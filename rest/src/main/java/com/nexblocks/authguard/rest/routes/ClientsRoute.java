package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.ApiKeyDTO;
import com.nexblocks.authguard.api.dto.entities.ClientDTO;
import com.nexblocks.authguard.api.dto.requests.CreateClientRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ClientsApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.IdempotencyHeader;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ClientsRoute extends ClientsApi {
    private final ClientsService clientsService;
    private final ApiKeysService apiKeysService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateClientRequestDTO> clientRequestRequestBodyHandler;

    @Inject
    public ClientsRoute(final ClientsService clientsService,
                        final ApiKeysService apiKeysService,
                        final RestMapper restMapper) {
        this.clientsService = clientsService;
        this.apiKeysService = apiKeysService;
        this.restMapper = restMapper;

        this.clientRequestRequestBodyHandler = new BodyHandler.Builder<>(CreateClientRequestDTO.class)
                .build();
    }

    public void create(final Context context) {
        String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        CreateClientRequestDTO request = clientRequestRequestBodyHandler.getValidated(context);

        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        CompletableFuture<ClientDTO> created = clientsService.create(restMapper.toBO(request), requestContext)
                .thenApply(restMapper::toDTO);

        context.future(() -> created.thenAccept(r -> context.status(201).json(r)));
    }

    @Override
    public void getByDomain(Context context) {
        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);

        CompletableFuture<List<ClientDTO>> clients = clientsService.getByDomain(Domain.fromContext(context), cursor)
                .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()));

        context.future(() -> clients.thenAccept(context::json));
    }

    public void getById(final Context context) {
        Validator<Long> clientId = context.pathParamAsClass("id", Long.class);

        if (!clientId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<ClientDTO> client = clientsService.getById(clientId.get(), Domain.fromContext(context))
                .thenApply(opt -> opt.map(restMapper::toDTO)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist")));

        context.future(() -> client.thenAccept(context::json));
    }

    public void getByExternalId(final Context context) {
        Validator<Long> clientId = context.pathParamAsClass("id", Long.class);

        if (!clientId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<ClientDTO> client = clientsService.getById(clientId.get(), Domain.fromContext(context))
                .thenApply(opt -> opt.map(restMapper::toDTO)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist")));

        context.future(() -> client.thenAccept(context::json));
    }

    public void deleteById(final Context context) {
        Validator<Long> clientId = context.pathParamAsClass("id", Long.class);

        if (!clientId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<ClientDTO> client = clientsService.delete(clientId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromClientOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> client.thenAccept(context::json));
    }

    public void activate(final Context context) {
        Validator<Long> clientId = context.pathParamAsClass("id", Long.class);

        if (!clientId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<ClientDTO> client = clientsService.activate(clientId.get(), Domain.fromContext(context))
                .thenApply(restMapper::toDTO);

        context.future(() -> client.thenAccept(context::json));
    }

    public void deactivate(final Context context) {
        Validator<Long> clientId = context.pathParamAsClass("id", Long.class);

        if (!clientId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<ClientDTO> client = clientsService.deactivate(clientId.get(), Domain.fromContext(context))
                .thenApply(restMapper::toDTO);

        context.future(() -> client.thenAccept(context::json));
    }

    @Override
    public void getApiKeys(final Context context) {
        Validator<Long> clientId = context.pathParamAsClass("id", Long.class);

        if (!clientId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<List<ApiKeyDTO>> keys = apiKeysService.getByAppId(clientId.get(), Domain.fromContext(context))
                .thenApply(list -> list
                        .stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> keys.thenAccept(context::json));
    }
}
