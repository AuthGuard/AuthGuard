package org.auther.service.impl;

import org.auther.service.PermissionsService;
import org.auther.service.RolesService;
import org.auther.service.model.PermissionBO;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The functionality of this class should be part of PermissionsRepository
 * but it's defined here temporarily.
 */
class PermissionsAggregator {
    private final RolesService rolesService;
    private final PermissionsService permissionsService;

    PermissionsAggregator(final RolesService rolesService, final PermissionsService permissionsService) {
        this.rolesService = rolesService;
        this.permissionsService = permissionsService;
    }

    List<PermissionBO> aggregate(final List<String> roles, final List<PermissionBO> accountPermissions) {
        final Stream<PermissionBO> rolesPermissions =  roles.stream()
                .map(rolesService::getPermissionsByName)
                .flatMap(Collection::stream);

        return expand(Stream.concat(accountPermissions.stream(), rolesPermissions));
    }

    private List<PermissionBO> expand(final Stream<PermissionBO> permissionsStream) {
        return permissionsStream.flatMap(permission -> {
                    if (permission.isWildCard()) {
                        return permissionsService.getAllForGroup(permission.getGroup())
                                .stream();
                    } else {
                        return Stream.of(permission);
                    }
                })
                .distinct()
                .collect(Collectors.toList());
    }
}
