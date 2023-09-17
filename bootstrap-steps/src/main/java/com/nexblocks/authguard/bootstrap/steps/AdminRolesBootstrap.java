package com.nexblocks.authguard.bootstrap.steps;

import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.model.RoleBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminRolesBootstrap implements BootstrapStep {
    private static final String RESERVED_DOMAIN = "global";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final RolesService rolesService;
    private final AccountConfig accountConfig;

    @Inject
    public AdminRolesBootstrap(final RolesService rolesService, final @Named("accounts") ConfigContext accountsConfig) {
        this.rolesService = rolesService;
        this.accountConfig = accountsConfig.asConfigBean(AccountConfig.class);
    }

    @Override
    public void run() {
        final String adminRoleName = accountConfig.getAuthguardAdminRole();

        if (adminRoleName == null) {
            throw new IllegalStateException("Admin role name cannot be null in accounts configuration");
        }

        if (rolesService.getRoleByName(adminRoleName, RESERVED_DOMAIN).isEmpty()) {
            log.info("Admin role {} wasn't found and will be created", adminRoleName);

            final RoleBO adminRole = createRole(adminRoleName);
            log.info("Created admin client role {}", adminRole);
        }
    }

    private RoleBO createRole(final String roleName) {
        final RoleBO role = RoleBO.builder()
                .name(roleName)
                .domain(RESERVED_DOMAIN)
                .build();

        return rolesService.create(role);
    }
}
