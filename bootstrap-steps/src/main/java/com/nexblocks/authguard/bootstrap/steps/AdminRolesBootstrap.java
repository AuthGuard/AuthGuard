package com.nexblocks.authguard.bootstrap.steps;

import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.config.ApplicationsConfig;
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
    private final ApplicationsConfig applicationsConfig;

    @Inject
    public AdminRolesBootstrap(final RolesService rolesService, final @Named("accounts") ConfigContext accountsConfig,
                               final @Named("apps") ConfigContext appsConfig) {
        this.rolesService = rolesService;
        this.accountConfig = accountsConfig.asConfigBean(AccountConfig.class);
        this.applicationsConfig = appsConfig.asConfigBean(ApplicationsConfig.class);
    }

    @Override
    public void run() {
        final String adminRoleName = accountConfig.getAuthguardAdminRole();
        final String adminClientRoleName = applicationsConfig.getAuthguardAdminClientRole();

        if (adminRoleName == null) {
            throw new IllegalStateException("Admin role name cannot be null in accounts configuration");
        }

        if (adminClientRoleName == null) {
            throw new IllegalStateException("Admin client role name cannot be null in applications configuration");
        }

        if (rolesService.getRoleByName(adminRoleName, RESERVED_DOMAIN).isEmpty()) {
            log.info("Admin role {} wasn't found and will be created", adminRoleName);

            final RoleBO adminRole = createRole(adminRoleName);
            log.info("Created admin client role {}", adminRole);
        }

        if (rolesService.getRoleByName(adminClientRoleName, RESERVED_DOMAIN).isEmpty()) {
            log.info("Admin client role {} wasn't found and will be created", adminClientRoleName);

            final RoleBO adminClientRole = createRole(adminClientRoleName);
            log.info("Created admin client role {}", adminClientRole);
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
