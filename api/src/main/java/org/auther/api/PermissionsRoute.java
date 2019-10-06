package org.auther.api;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.PermissionDTO;
import org.auther.api.dto.PermissionGroupDTO;
import org.auther.service.PermissionsService;
import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class PermissionsRoute implements EndpointGroup {
    private final PermissionsService permissionsService;
    private final RestMapper restMapper;

    @Inject
    PermissionsRoute(final PermissionsService permissionsService, final RestMapper restMapper) {
        this.permissionsService = permissionsService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/permissions/groups", this::createGroup);
        post("/permissions", this::createPermission);
        get("/permissions", this::getPermissions);
        delete("/permissions", this::deletePermission);
    }

    private void createGroup(final Context context) {
        final PermissionGroupDTO permissionGroup = context.bodyAsClass(PermissionGroupDTO.class);

        Optional.of(permissionGroup)
                .map(restMapper::toBO)
                .map(permissionsService::createPermissionGroup)
                .map(restMapper::toDTO)
                .map(context::json);
    }

    private void createPermission(final Context context) {
        final PermissionDTO permissionGroup = context.bodyAsClass(PermissionDTO.class);

        Optional.of(permissionGroup)
                .map(restMapper::toBO)
                .map(permissionsService::createPermission)
                .map(restMapper::toDTO)
                .map(context::json);
    }

    private void getPermissions(final Context context) {
        final String groupName = context.queryParam("group");

        if (groupName == null) {
            final List<PermissionDTO> permissions = permissionsService.getPermissions().stream()
                    .map(restMapper::toDTO)
                    .collect(Collectors.toList());

            context.json(permissions);
        } else {
            final Optional<List<PermissionBO>> permissions = permissionsService.getPermissionsByGroup(groupName);

            if (permissions.isPresent()) {
                context.json(
                        permissions.get().stream()
                                .map(restMapper::toDTO)
                                .collect(Collectors.toList())
                );
            } else {
                context.status(404).result("No permission group " + groupName + " found");
            }
        }
    }

    private void deletePermission(final Context context) {
        final PermissionDTO permissionGroup = context.bodyAsClass(PermissionDTO.class);

        final Optional<PermissionBO> permission = Optional.of(permissionGroup)
                .map(restMapper::toBO)
                .flatMap(permissionsService::deletePermission);

        if (permission.isPresent()) {
            context.json(restMapper.toDTO(permission.get()));
        } else {
            context.status(404).result("");
        }
    }
}
