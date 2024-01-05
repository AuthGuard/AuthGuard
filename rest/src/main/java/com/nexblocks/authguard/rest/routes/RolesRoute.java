package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.RoleDTO;
import com.nexblocks.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.RolesApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.core.validation.Validator;
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

    public void create(final Context context) {
        CreateRoleRequestDTO role = createRoleRequestBodyHandler.getValidated(context);

        CompletableFuture<RoleDTO> created = rolesService.create(restMapper.toBO(role))
                .thenApply(restMapper::toDTO);

        context.status(201).json(created);
    }

    @Override
    public void getById(final Context context) {
        Validator<Long> id = context.pathParam("id", Long.class);

        if (!id.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<RoleDTO> role = rolesService.getById(id.get())
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO);

        context.json(role);
    }

    @Override
    public void deleteById(final Context context) {
        Validator<Long> id = context.pathParam("id", Long.class);

        if (!id.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<RoleDTO> role = rolesService.delete(id.get())
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO);

        context.json(role);
    }

    public void getByName(final Context context) {
        String name = context.pathParam("name");
        String domain = context.pathParam("domain");

        CompletableFuture<RoleDTO> role = rolesService.getRoleByName(name, domain)
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.ROLE_DOES_NOT_EXIST, "Role does not exist"))
                .thenApply(restMapper::toDTO);

        context.json(role);
    }

    @Override
    public void getAll(final Context context) {
        String domain = context.pathParam("domain");

        CompletableFuture<List<RoleDTO>> roles = rolesService.getAll(domain)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.json(roles);
    }
}
