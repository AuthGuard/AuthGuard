package org.auther.rest.routes;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.rest.dto.PermissionDTO;
import org.auther.service.PermissionsService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

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
        post("/permissions", this::createPermission);
        get("/permissions", this::getPermissions);
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
