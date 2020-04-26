package com.authguard.bootstrap.steps;

import com.authguard.bootstrap.BootstrapStep;
import com.authguard.service.RolesService;
import com.authguard.service.model.RoleBO;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminRolesBootstrap implements BootstrapStep {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final RolesService rolesService;

    @Inject
    public AdminRolesBootstrap(final RolesService rolesService) {
        this.rolesService = rolesService;
    }

    @Override
    public void run() {
        final RoleBO adminRole = createRole("authguard_admin");
        log.info("Created admin client role {}", adminRole);

        final RoleBO adminClientRole = createRole("authguard_admin_client");
        log.info("Created admin client role {}", adminClientRole);
    }

    private RoleBO createRole(final String roleName) {
        final RoleBO role = RoleBO.builder()
                .name(roleName)
                .build();

        return rolesService.create(role);
    }
}
