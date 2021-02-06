package com.authguard.rest.routes;

import com.authguard.api.dto.entities.Error;
import com.authguard.api.dto.entities.RoleDTO;
import com.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.authguard.api.routes.RolesApi;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.util.BodyHandler;
import com.authguard.service.RolesService;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;
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
        final CreateRoleRequestDTO role = createRoleRequestBodyHandler.getValidated(context);

        final RoleDTO created = Optional.of(role)
                .map(restMapper::toBO)
                .map(rolesService::create)
                .map(restMapper::toDTO)
                .orElseThrow();

        context.status(201).json(created);
    }

    @Override
    public void getById(final Context context) {
        final String id = context.pathParam("id");

        rolesService.getById(id)
                .map(restMapper::toDTO)
                .ifPresentOrElse(
                        role -> context.status(200).json(role),
                        // or else
                        () -> context.status(404)
                                .json(new Error(ErrorCode.ROLE_DOES_NOT_EXIST.getCode(),
                                        "No role with ID " + id + " exists"))
                );
    }

    @Override
    public void deleteById(final Context context) {
        final String id = context.pathParam("id");

        rolesService.getById(id)
                .map(restMapper::toDTO)
                .ifPresentOrElse(
                        role -> context.status(200).json(role),
                        // or else
                        () -> context.status(404)
                                .json(new Error(ErrorCode.ROLE_DOES_NOT_EXIST.getCode(),
                                        "No role with ID " + id + " exists"))
                );
    }

    public void getByName(final Context context) {
        final String name = context.pathParam("name");

        rolesService.getRoleByName(name)
                .map(restMapper::toDTO)
                .ifPresentOrElse(
                        role -> context.status(200).json(role),
                        // or else
                        () -> context.status(404)
                                .json(new Error(ErrorCode.ROLE_DOES_NOT_EXIST.getCode(),
                                        "No role with name " + name + " exists"))
                );
    }

    @Override
    public void getAll(final Context context) {
        final List<RoleDTO> roles = rolesService.getAll().stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.json(roles);
    }
}
