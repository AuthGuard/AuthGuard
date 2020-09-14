package com.authguard.rest.routes;

import com.authguard.api.dto.entities.PermissionDTO;
import com.authguard.api.routes.PermissionsApi;
import com.authguard.rest.mappers.RestJsonMapper;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.service.PermissionsService;
import com.authguard.service.model.PermissionBO;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionsRoute extends PermissionsApi {
    private final PermissionsService permissionsService;
    private final RestMapper restMapper;

    @Inject
    public PermissionsRoute(final PermissionsService permissionsService, final RestMapper restMapper) {
        this.permissionsService = permissionsService;
        this.restMapper = restMapper;
    }

    public void createPermission(final Context context) {
        final PermissionDTO permission = RestJsonMapper.asClass(context.body(), PermissionDTO.class);
        final PermissionBO created = permissionsService.create(restMapper.toBO(permission));

        context.status(201)
                .json(restMapper.toDTO(created));
    }

    public void getPermissions(final Context context) {
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
