package com.authguard.rest.routes;

import com.authguard.api.dto.entities.PermissionDTO;
import com.authguard.rest.access.ActorRoles;
import com.authguard.service.PermissionsService;
import com.authguard.service.model.PermissionBO;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class PermissionsRoute implements EndpointGroup {
    private final PermissionsService permissionsService;
    private final RestMapper restMapper;

    @Inject
    public PermissionsRoute(final PermissionsService permissionsService, final RestMapper restMapper) {
        this.permissionsService = permissionsService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/", this::createPermission, ActorRoles.adminClient());
        get("/", this::getPermissions, ActorRoles.adminClient());
    }

    private void createPermission(final Context context) {
        final PermissionDTO permission = RestJsonMapper.asClass(context.body(), PermissionDTO.class);
        final PermissionBO created = permissionsService.create(restMapper.toBO(permission));

        context.status(201)
                .json(restMapper.toDTO(created));
    }

    private void getPermissions(final Context context) {
        final String groupName = context.queryParam("group");

        if (groupName == null) {
            final List<PermissionDTO> permissions = permissionsService.getAll().stream()
                    .map(restMapper::toDTO)
                    .collect(Collectors.toList());

            context.json(permissions);
        } else {
            final Collection<PermissionDTO> permissions = permissionsService.getAllForGroup(groupName).
                    stream()
                    .map(restMapper::toDTO)
                    .collect(Collectors.toList());

            context.status(200).json(permissions);
        }
    }
}
