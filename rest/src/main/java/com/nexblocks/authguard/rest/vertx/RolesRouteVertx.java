package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.Cursors;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UpdateRoleRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.stream.Collectors;

public class RolesRouteVertx implements VertxApiHandler {
    private final RolesService rolesService;
    private final RestMapper restMapper;
    private final BodyHandler<CreateRoleRequestDTO> createRoleRequestBodyHandler;

    @Inject
    public RolesRouteVertx(final RolesService rolesService, final RestMapper restMapper) {
        this.rolesService = rolesService;
        this.restMapper = restMapper;
        this.createRoleRequestBodyHandler = new BodyHandler.Builder<>(CreateRoleRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/roles").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::create);
        router.get("/domains/:domain/roles/:id").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getById);
        router.delete("/domains/:domain/roles/:id").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::deleteById);
        router.get("/domains/:domain/roles/name/:name").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getByName);
        router.get("/domains/:domain/roles").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getAll);
        router.patch("/domains/:domain/roles/:id").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::update);
    }

    private void create(final RoutingContext context) {
        try {
            CreateRoleRequestDTO role = createRoleRequestBodyHandler.getValidated(context);

            rolesService.create(restMapper.toBO(role))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().setStatusCode(201).putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getById(final RoutingContext context) {
        try {
            long id = Long.parseLong(context.pathParam("id"));
            String domain = context.pathParam("domain");

            rolesService.getById(id, domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
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

            rolesService.delete(id, domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }

    private void getByName(final RoutingContext context) {
        String name = context.pathParam("name");
        String domain = context.pathParam("domain");

        rolesService.getRoleByName(name, domain)
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                });
    }

    private void getAll(final RoutingContext context) {
        String domain = context.pathParam("domain");
        Long cursor = Cursors.getLongCursor(context);

        rolesService.getAll(domain, cursor)
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

            UpdateRoleRequestDTO role = context.body().asPojo(UpdateRoleRequestDTO.class);

            rolesService.update(restMapper.toBO(role).withId(id), domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        } catch (Exception e) {
            context.fail(e);
        }
    }
}

