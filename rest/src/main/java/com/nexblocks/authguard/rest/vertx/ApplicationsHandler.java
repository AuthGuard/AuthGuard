package com.nexblocks.authguard.rest.vertx;

import com.nexblocks.authguard.api.common.*;
import com.nexblocks.authguard.api.dto.entities.AppDTO;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.KeyManagementService;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
import java.util.stream.Collectors;

public class ApplicationsHandler implements VertxApiHandler {
    private final ApplicationsService applicationsService;
    private final ApiKeysService apiKeysService;
    private final KeyManagementService keyManagementService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAppRequestDTO> appRequestBodyHandler;
    private final BodyHandler<PermissionsRequestDTO> permissionsBodyHandler;
    private final BodyHandler<RolesRequestDTO> rolesBodyHandler;

    @Inject
    public ApplicationsHandler(final ApplicationsService applicationsService,
                               final ApiKeysService apiKeysService,
                               final KeyManagementService keyManagementService,
                               final RestMapper restMapper) {
        this.applicationsService = applicationsService;
        this.apiKeysService = apiKeysService;
        this.keyManagementService = keyManagementService;
        this.restMapper = restMapper;

        this.appRequestBodyHandler = new BodyHandler.Builder<>(CreateAppRequestDTO.class).build();
        this.permissionsBodyHandler = new BodyHandler.Builder<>(PermissionsRequestDTO.class).build();
        this.rolesBodyHandler = new BodyHandler.Builder<>(RolesRequestDTO.class).build();
    }

    public void register(Router router) {
        router.post("/domains/:domain/apps").handler(this::create);
        router.get("/domains/:domain/apps/:id").handler(this::getById);
        router.get("/domains/:domain/apps/externalId/:id").handler(this::getByExternalId);
        router.put("/domains/:domain/apps/:id").handler(this::update);
        router.delete("/domains/:domain/apps/:id").handler(this::delete);
        router.patch("/domains/:domain/apps/:id/activate").handler(this::activate);
        router.patch("/domains/:domain/apps/:id/deactivate").handler(this::deactivate);
        router.patch("/domains/:domain/apps/:id/permissions").handler(this::updatePermissions);
        router.patch("/domains/:domain/apps/:id/roles").handler(this::updateRoles);
        router.get("/domains/:domain/apps/:id/keys").handler(this::getApiKeys);
        router.get("/domains/:domain/apps/:id/crypto_keys").handler(this::getCryptoKeys);
    }

    private void create(RoutingContext context) {
        String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        CreateAppRequestDTO request = appRequestBodyHandler.getValidated(context);

        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.request().remoteAddress().host())
                .build();

        applicationsService.create(restMapper.toBO(request), requestContext)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context, 201));
    }

    private void getById(RoutingContext context) {
        long id = parseIdOrFail(context);
        applicationsService.getById(id, Domain.fromContext(context))
                .flatMap(AsyncUtils::uniFromAppOptional)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private void getByExternalId(RoutingContext context) {
        long id = parseIdOrFail(context);
        applicationsService.getByExternalId(id, Domain.fromContext(context))
                .flatMap(AsyncUtils::uniFromAppOptional)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void update(RoutingContext context) {
        AppDTO request = context.body().asPojo(AppDTO.class);

        applicationsService.update(restMapper.toBO(request), Domain.fromContext(context))
                .flatMap(AsyncUtils::uniFromAppOptional)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void delete(RoutingContext context) {
        long id = parseIdOrFail(context);
        applicationsService.delete(id, Domain.fromContext(context))
                .flatMap(AsyncUtils::uniFromAppOptional)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void activate(RoutingContext context) {
        long id = parseIdOrFail(context);
        applicationsService.activate(id, Domain.fromContext(context))
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void deactivate(RoutingContext context) {
        long id = parseIdOrFail(context);
        applicationsService.deactivate(id, Domain.fromContext(context))
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void updatePermissions(RoutingContext context) {
        long id = parseIdOrFail(context);
        PermissionsRequestDTO request = permissionsBodyHandler.getValidated(context);

        List<PermissionBO> permissions = request.getPermissions().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        Uni<AppBO> future = request.getAction() == PermissionsRequest.Action.GRANT ?
                applicationsService.grantPermissions(id, permissions, Domain.fromContext(context)) :
                applicationsService.revokePermissions(id, permissions, Domain.fromContext(context));

        future.map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void updateRoles(RoutingContext context) {
        long id = parseIdOrFail(context);
        RolesRequestDTO request = rolesBodyHandler.getValidated(context);

        Uni<Optional<AppBO>> future = request.getAction() == RolesRequest.Action.GRANT ?
                applicationsService.grantRoles(id, request.getRoles(), Domain.fromContext(context)) :
                applicationsService.revokeRoles(id, request.getRoles(), Domain.fromContext(context));

        future.flatMap(AsyncUtils::uniFromAppOptional)
                .map(restMapper::toDTO)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void getApiKeys(RoutingContext context) {
        long id = parseIdOrFail(context);
        apiKeysService.getByAppId(id, Domain.fromContext(context))
                .map(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private void getCryptoKeys(RoutingContext context) {
        long id = parseIdOrFail(context);
        Long cursor = parseCursor(context);
        Instant instantCursor = Cursors.parseInstantCursor(cursor);

        keyManagementService.getByAccountId(Domain.fromContext(context), id, instantCursor)
                .map(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));;
    }

    private long parseIdOrFail(RoutingContext context) {
        try {
            return Long.parseLong(context.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("id", ViolationType.INVALID_VALUE)
            ));
        }
    }

    private Long parseCursor(RoutingContext context) {
        String cursor = context.queryParam("cursor").stream()
                .findAny()
                .orElse(null);

        if (cursor != null) {
            try {
                return Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private <T> void jsonResponse(RoutingContext context, T body) {
        jsonResponse(context, 200, body);
    }
    
    private <T> void jsonResponse(RoutingContext context, int statusCode, T body) {
        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(body));
    }
}

