package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.Cursors;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.requests.CreatePermissionRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UpdatePermissionRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.stream.Collectors;

public class PermissionsHandler implements VertxApiHandler {
    private final PermissionsService permissionsService;
    private final RestMapper restMapper;
    private final BodyHandler<CreatePermissionRequestDTO> createPermissionRequestBodyHandler;

    @Inject
    public PermissionsHandler(final PermissionsService permissionsService, final RestMapper restMapper) {
        this.permissionsService = permissionsService;
        this.restMapper = restMapper;
        this.createPermissionRequestBodyHandler = new BodyHandler.Builder<>(CreatePermissionRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/permissions")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::create);

        router.get("/domains/:domain/permissions/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getById);

        router.delete("/domains/:domain/permissions/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::deleteById);

        router.get("/domains/:domain/permissions/group/:group")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getByGroup);

        router.get("/domains/:domain/permissions")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getAll);

        router.patch("/domains/:domain/permissions/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::update);
    }

    private void create(final RoutingContext context) {
        CreatePermissionRequestDTO request = createPermissionRequestBodyHandler.getValidated(context);

        permissionsService.create(restMapper.toBO(request))
                .thenApply(restMapper::toDTO)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().setStatusCode(201)
                            .putHeader("Content-Type", "application/json")
                            .end(Json.encode(res));
                });
    }

    private void getById(final RoutingContext context) {
        try {
            long id = Long.parseLong(context.pathParam("id"));
            String domain = context.pathParam("domain");

            permissionsService.getById(id, domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.PERMISSION_DOES_NOT_EXIST, "Permission does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }

    private void deleteById(final RoutingContext context) {
        try {
            long id = Long.parseLong(context.pathParam("id"));
            String domain = context.pathParam("domain");

            permissionsService.delete(id, domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.PERMISSION_DOES_NOT_EXIST, "Permission does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }

    private void getByGroup(final RoutingContext context) {
        String group = context.pathParam("group");
        String domain = context.pathParam("domain");
        Long cursor = Cursors.getLongCursor(context);

        permissionsService.getAllForGroup(group, domain, cursor)
                .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                });
    }

    private void getAll(final RoutingContext context) {
        String domain = context.pathParam("domain");
        Long cursor = Cursors.getLongCursor(context);

        permissionsService.getAll(domain, cursor)
                .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                });
    }

    private void update(final RoutingContext context) {
        try {
            long id = Long.parseLong(context.pathParam("id"));
            String domain = context.pathParam("domain");

            UpdatePermissionRequestDTO request = Json.decodeValue(context.body().asString(), UpdatePermissionRequestDTO.class);

            permissionsService.update(restMapper.toBO(request).withId(id), domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.PERMISSION_DOES_NOT_EXIST, "Permission does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }
}

