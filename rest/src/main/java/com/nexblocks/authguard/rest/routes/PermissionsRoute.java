package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.requests.CreatePermissionRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UpdatePermissionRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.PermissionsApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class PermissionsRoute extends PermissionsApi {
    private final PermissionsService permissionsService;
    private final RestMapper restMapper;

    private final BodyHandler<CreatePermissionRequestDTO> createPermissionRequestBodyHandler;

    @Inject
    public PermissionsRoute(final PermissionsService permissionsService, final RestMapper restMapper) {
        this.permissionsService = permissionsService;
        this.restMapper = restMapper;

        this.createPermissionRequestBodyHandler = new BodyHandler.Builder<>(CreatePermissionRequestDTO.class)
                .build();
    }

    @Override
    public String getPath() {
        return "/domains/{domain}/permissions";
    }

    @Override
    public void addEndpoints() {
        post("/", this::create, ActorRoles.adminClient());
        get("/{id}", this::getById, ActorRoles.adminClient());
        delete("/{id}", this::getById, ActorRoles.adminClient());
        get("/group/{group}", this::getByGroup, ActorRoles.adminClient());
        get("", this::getAll, ActorRoles.adminClient());
        patch("/{id}", this::update, ActorRoles.adminClient());
    }

    public void create(final Context context) {
        CreatePermissionRequestDTO permission = createPermissionRequestBodyHandler.getValidated(context);
        CompletableFuture<PermissionDTO> created = permissionsService.create(restMapper.toBO(permission))
                .thenApply(restMapper::toDTO);

        context.future(() -> created.thenAccept(r -> context.status(201).json(r)));
    }

    @Override
    public void getById(final Context context) {
        Validator<Long> id = context.pathParamAsClass("id", Long.class);

        if (!id.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<PermissionDTO> permission = permissionsService.getById(id.get(), Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.PERMISSION_DOES_NOT_EXIST, "Permission does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> permission.thenAccept(context::json));
    }

    @Override
    public void deleteById(final Context context) {
        Validator<Long> id = context.pathParamAsClass("id", Long.class);

        if (!id.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<PermissionDTO> permission = permissionsService.delete(id.get(), Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.PERMISSION_DOES_NOT_EXIST, "Permission does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> permission.thenAccept(context::json));
    }

    @Override
    public void getByGroup(final Context context) {
        String group = context.pathParam("group");
        String domain = context.pathParam("domain");
        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);

        CompletableFuture<List<PermissionDTO>> permissions = permissionsService.getAllForGroup(group, domain, cursor)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> permissions.thenAccept(context::json));
    }

    public void getAll(final Context context) {
        String domain = context.pathParam("domain");
        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);

        CompletableFuture<List<PermissionDTO>> permissions = permissionsService.getAll(domain, cursor)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> permissions.thenAccept(context::json));
    }

    @Override
    public void update(Context context) {
        Validator<Long> id = context.pathParamAsClass("id", Long.class);

        if (!id.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        UpdatePermissionRequestDTO role = context.bodyAsClass(UpdatePermissionRequestDTO.class);

        CompletableFuture<PermissionDTO> created = permissionsService.update(restMapper.toBO(role).withId(id.get()),
                        Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.PERMISSION_DOES_NOT_EXIST, "Permission does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> created.thenAccept(context::json));
    }
}
