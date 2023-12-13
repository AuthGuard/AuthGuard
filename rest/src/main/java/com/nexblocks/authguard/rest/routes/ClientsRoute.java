package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.ApiKeyDTO;
import com.nexblocks.authguard.api.dto.entities.ClientDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.requests.CreateClientRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ClientsApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        final String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        final CreateClientRequestDTO request = clientRequestRequestBodyHandler.getValidated(context);

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        final Optional<Object> created = Optional.of(restMapper.toBO(request))
                .map(clientBO -> clientsService.create(clientBO, requestContext))
                .map(restMapper::toDTO);

        if (created.isPresent()) {
            context.status(201).json(created.get());
        } else {
            context.status(400).json(new Error("400", "Failed to create client"));
        }
    }

    public void getById(final Context context) {
        final Validator<Long> clientId = context.pathParam("id", Long.class);

        if (!clientId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final Optional<ClientDTO> client = clientsService.getById(clientId.get())
                .map(restMapper::toDTO);

        if (client.isPresent()) {
            context.status(200).json(client.get());
        } else {
            context.status(404);
        }
    }

    public void getByExternalId(final Context context) {
        final Validator<Long> clientId = context.pathParam("id", Long.class);

        if (!clientId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final Optional<ClientDTO> client = clientsService.getById(clientId.get())
                .map(restMapper::toDTO);

        if (client.isPresent()) {
            context.status(200).json(client.get());
        } else {
            context.status(404);
        }
    }

    public void deleteById(final Context context) {
        final Validator<Long> clientId = context.pathParam("id", Long.class);

        if (!clientId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final Optional<ClientDTO> client = clientsService.delete(clientId.get())
                .map(restMapper::toDTO);

        if (client.isPresent()) {
            context.status(200).json(client.get());
        } else {
            context.status(404);
        }
    }

    public void activate(final Context context) {
        final Validator<Long> clientId = context.pathParam("id", Long.class);

        if (!clientId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final Optional<ClientDTO> client = clientsService.activate(clientId.get())
                .map(restMapper::toDTO);

        if (client.isPresent()) {
            context.status(200).json(client.get());
        } else {
            context.status(404);
        }
    }

    public void deactivate(final Context context) {
        final Validator<Long> clientId = context.pathParam("id", Long.class);

        if (!clientId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final Optional<ClientDTO> client = clientsService.deactivate(clientId.get())
                .map(restMapper::toDTO);

        if (client.isPresent()) {
            context.status(200).json(client.get());
        } else {
            context.status(404);
        }
    }

    @Override
    public void getApiKeys(final Context context) {
        final Validator<Long> clientId = context.pathParam("id", Long.class);

        if (!clientId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        final List<ApiKeyDTO> keys = apiKeysService.getByAppId(clientId.get())
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.status(200).json(keys);
    }
}
