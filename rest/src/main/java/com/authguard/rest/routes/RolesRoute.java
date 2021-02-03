package com.authguard.rest.routes;

import com.authguard.api.dto.entities.RoleDTO;
import com.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.authguard.api.routes.RolesApi;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.util.BodyHandler;
import com.authguard.service.RolesService;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.Optional;

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

    public void getByName(final Context context) {
        final String name = context.pathParam("name");

        final Optional<RoleDTO> role = rolesService.getRoleByName(name)
                .map(restMapper::toDTO);

        if (role.isPresent()) {
            context.status(200).json(role.get());
        } else {
            context.status(404);
        }
    }
}
