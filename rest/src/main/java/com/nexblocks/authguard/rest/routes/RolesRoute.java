package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.RoleDTO;
import com.nexblocks.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UpdateRoleRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.RolesApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RolesRoute extends RolesApi {
    private final RolesService rolesService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateRoleRequestDTO> createRoleRequestBodyHandler;

    @Inject
    public RolesRoute(final RolesService rolesService, final RestMapper restMapper) {
        this.rolesService = rolesService;
        this.restMapper = restMapper;

        this.createRoleRequestBodyHandler = new BodyHandler.Builder<>(CreateRoleRequestDTO.class)
                .build();
    }

    @Override
    public void update(Context context) {
        Validator<Long> id = context.pathParamAsClass("id", Long.class);

        if (!id.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        UpdateRoleRequestDTO role = context.bodyAsClass(UpdateRoleRequestDTO.class);

        CompletableFuture<RoleDTO> created = rolesService.update(restMapper.toBO(role).withId(id.get()),
                        Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> created.thenAccept(context::json));
    }

    public void create(final Context context) {
        CreateRoleRequestDTO role = createRoleRequestBodyHandler.getValidated(context);

        CompletableFuture<RoleDTO> created = rolesService.create(restMapper.toBO(role))
                .thenApply(restMapper::toDTO);

        context.future(() -> created.thenAccept(r -> context.status(201).json(r)));
    }

    @Override
    public void getById(final Context context) {
        Validator<Long> id = context.pathParamAsClass("id", Long.class);

        if (!id.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<RoleDTO> role = rolesService.getById(id.get(), Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> role.thenAccept(context::json));
    }

    @Override
    public void deleteById(final Context context) {
        Validator<Long> id = context.pathParamAsClass("id", Long.class);

        if (!id.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<RoleDTO> role = rolesService.delete(id.get(), Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> role.thenAccept(context::json));
    }

    public void getByName(final Context context) {
        String name = context.pathParam("name");
        String domain = context.pathParam("domain");

        CompletableFuture<RoleDTO> role = rolesService.getRoleByName(name, domain)
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> role.thenAccept(context::json));
    }

    @Override
    public void getAll(final Context context) {
        String domain = context.pathParam("domain");
        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);

        CompletableFuture<List<RoleDTO>> roles = rolesService.getAll(domain, cursor)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> roles.thenAccept(context::json));
    }
}
