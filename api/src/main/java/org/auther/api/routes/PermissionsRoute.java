package org.auther.api.routes;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.PermissionDTO;
import org.auther.api.dto.PermissionGroupDTO;
import org.auther.service.PermissionsServices;
import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.get;

public class PermissionsRoute implements EndpointGroup {
    private final PermissionsServices permissionsServices;
    private final RestMapper restMapper;

    public PermissionsRoute(final PermissionsServices permissionsServices) {
        this.permissionsServices = permissionsServices;
        this.restMapper = RestMapper.INSTANCE;
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
                .map(permissionsServices::createPermissionGroup)
                .map(restMapper::toDTO)
                .map(context::json);
    }

    private void createPermission(final Context context) {
        final PermissionDTO permissionGroup = context.bodyAsClass(PermissionDTO.class);

        Optional.of(permissionGroup)
                .map(restMapper::toBO)
                .map(permissionsServices::createPermission)
                .map(restMapper::toDTO)
                .map(context::json);
    }

    private void getPermissions(final Context context) {
        final String groupName = context.queryParam("group");

        if (groupName == null) {
            final List<PermissionDTO> permissions = permissionsServices.getPermissions().stream()
                    .map(restMapper::toDTO)
                    .collect(Collectors.toList());

            context.json(permissions);
        } else {
            final Optional<List<PermissionBO>> permissions = permissionsServices.getPermissionsByGroup(groupName);

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
                .flatMap(permissionsServices::deletePermission);

        if (permission.isPresent()) {
            context.json(restMapper.toDTO(permission.get()));
        } else {
            context.status(404).result("");
        }
    }
}
