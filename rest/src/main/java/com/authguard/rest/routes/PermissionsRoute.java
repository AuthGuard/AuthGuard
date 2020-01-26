package com.authguard.rest.routes;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import com.authguard.rest.access.ActorRole;
import com.authguard.rest.dto.PermissionDTO;
import com.authguard.service.PermissionsService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.core.security.SecurityUtil.roles;

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
        post("/permissions", this::createPermission, roles(ActorRole.of("admin")));
        get("/permissions", this::getPermissions, roles(ActorRole.of("admin")));
    }

    private void createPermission(final Context context) {
        final PermissionDTO permissionGroup = context.bodyAsClass(PermissionDTO.class);

        Optional.of(permissionGroup)
                .map(restMapper::toBO)
                .map(permissionsService::create)
                .map(restMapper::toDTO)
                .map(context::json);
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
