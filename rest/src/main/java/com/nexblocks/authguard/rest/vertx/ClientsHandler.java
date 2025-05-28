package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.common.IdempotencyHeader;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.requests.CreateClientRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Collections;
import java.util.stream.Collectors;

public class ClientsHandler implements VertxApiHandler {
    private final ClientsService clientsService;
    private final ApiKeysService apiKeysService;
    private final RestMapper restMapper;

    @Inject
    public ClientsHandler(final ClientsService clientsService,
                        final ApiKeysService apiKeysService,
                        final RestMapper restMapper) {
        this.clientsService = clientsService;
        this.apiKeysService = apiKeysService;
        this.restMapper = restMapper;
    }

    public void register(final Router router) {
        router.post("/domains/:domain/clients").handler(BodyHandler.create()).handler(this::create);
        router.get("/domains/:domain/clients").handler(this::getByDomain);
        router.get("/domains/:domain/clients/:id").handler(this::getById);
        router.get("/domains/:domain/clients/externalId/:id").handler(this::getByExternalId);
        router.delete("/domains/:domain/clients/:id").handler(this::deleteById);
        router.patch("/domains/:domain/clients/:id/activate").handler(this::activate);
        router.patch("/domains/:domain/clients/:id/deactivate").handler(this::deactivate);
        router.get("/domains/:domain/clients/:id/keys").handler(this::getApiKeys);
    }

    private void create(final RoutingContext context) {
        String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        CreateClientRequestDTO request = Json.decodeValue(context.body().asString(), CreateClientRequestDTO.class);

        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.request().remoteAddress().host())
                .build();

        clientsService.create(restMapper.toBO(request), requestContext)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context, 201));
    }

    private void getByDomain(final RoutingContext context) {
        Long cursor = context.queryParam("cursor").isEmpty() ? null : Long.valueOf(context.queryParam("cursor").get(0));

        clientsService.getByDomain(Domain.fromContext(context), cursor)
                .map(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private void getById(final RoutingContext context) {
        long clientId = parseIdParam(context);

        clientsService.getById(clientId, Domain.fromContext(context))
                .map(opt -> opt.map(restMapper::toDTO)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist")))
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private void getByExternalId(final RoutingContext context) {
        long clientId = parseIdParam(context);

        clientsService.getById(clientId, Domain.fromContext(context))
                .map(opt -> opt.map(restMapper::toDTO)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist")))
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private void deleteById(final RoutingContext context) {
        long clientId = parseIdParam(context);

        clientsService.delete(clientId, Domain.fromContext(context))
                .flatMap(AsyncUtils::uniFromClientOptional)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private void activate(final RoutingContext context) {
        long clientId = parseIdParam(context);

        clientsService.activate(clientId, Domain.fromContext(context))
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private void deactivate(final RoutingContext context) {
        long clientId = parseIdParam(context);

        clientsService.deactivate(clientId, Domain.fromContext(context))
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private void getApiKeys(final RoutingContext context) {
        long clientId = parseIdParam(context);

        apiKeysService.getByAppId(clientId, Domain.fromContext(context))
                .map(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private long parseIdParam(final RoutingContext context) {
        try {
            return Long.parseLong(context.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("id", ViolationType.INVALID_VALUE)));
        }
    }
}

