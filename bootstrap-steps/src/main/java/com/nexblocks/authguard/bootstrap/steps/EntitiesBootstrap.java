package com.nexblocks.authguard.bootstrap.steps;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.BootstrapEntitiesConfig;
import com.nexblocks.authguard.service.config.PermissionConfig;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RoleBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class EntitiesBootstrap implements BootstrapStep {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final RolesService rolesService;
    private final PermissionsService permissionsService;

    private final BootstrapEntitiesConfig entitiesConfig;

    @Inject
    public EntitiesBootstrap(final RolesService rolesService,
                             final PermissionsService permissionsService,
                             final @Named("entities") ConfigContext entitiesConfig) {
        this(rolesService, permissionsService, entitiesConfig == null
                ? null
                : entitiesConfig.asConfigBean(BootstrapEntitiesConfig.class));
    }

    public EntitiesBootstrap(final RolesService rolesService,
                             final PermissionsService permissionsService,
                             final BootstrapEntitiesConfig entitiesConfig) {
        this.rolesService = rolesService;
        this.permissionsService = permissionsService;
        this.entitiesConfig = entitiesConfig;
    }

    @Override
    public void run() {
        if (entitiesConfig == null || entitiesConfig.getDomains() == null) {
            log.info("No entities config was provided. Skipping.");

            return;
        }

        entitiesConfig.getDomains().forEach((domain, entities) -> {
           createRoles(domain, entities.getRoles());

           createPermissions(domain, entities.getPermissions());
        });
    }

    private void createRoles(String domain, List<String> roles) {
        if (roles == null) {
            return;
        }

        roles.stream()
                .map(roleName -> RoleBO.builder().domain(domain).name(roleName).build())
                .forEach(role -> {
                    if (rolesService.getRoleByName(role.getName(), domain).isPresent()) {
                        log.info("Role {} already exists in domain {}", role.getName(), domain);

                        return;
                    }

                    rolesService.create(role);

                    log.info("Created role {} in domain {}", role.getName(), domain);
                });
    }

    private void createPermissions(String domain, List<PermissionConfig> permissions) {
        if (permissions == null) {
            return;
        }

        final List<PermissionBO> permissionBOS = permissions.stream()
                .map(permission -> PermissionBO.builder()
                        .group(permission.getGroup())
                        .name(permission.getName())
                        .build())
                .collect(Collectors.toList());

        final List<PermissionBO> existing = permissionsService.validate(permissionBOS, domain)
                .stream()
                .map(permission -> PermissionBO.builder()
                        .group(permission.getGroup())
                        .name(permission.getName())
                        .build())
                .collect(Collectors.toList());

        if (existing.size() == permissionBOS.size()) {
            log.info("No new permissions to create for domain {}", domain);
        }

        final List<PermissionBO> difference = permissionBOS.stream()
                .filter(permission -> !existing.contains(permission))
                .collect(Collectors.toList());

        difference.forEach(permission -> {
            permissionsService.create(permission.withDomain(domain));

            log.info("Created permission {}:{} in domain {}", permission.getGroup(), permission.getName(), domain);
        });
    }
}
