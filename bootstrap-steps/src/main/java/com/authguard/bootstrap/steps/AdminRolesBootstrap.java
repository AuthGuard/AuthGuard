package com.authguard.bootstrap.steps;

import com.authguard.bootstrap.BootstrapStep;
import com.authguard.config.ConfigContext;
import com.authguard.service.RolesService;
import com.authguard.service.config.AccountConfig;
import com.authguard.service.config.ApplicationsConfig;
import com.authguard.service.model.RoleBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminRolesBootstrap implements BootstrapStep {
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

        if (rolesService.getRoleByName(adminRoleName).isEmpty()) {
            log.info("Admin role {} wasn't found and will be created", adminRoleName);

            final RoleBO adminRole = createRole(adminRoleName);
            log.info("Created admin client role {}", adminRole);
        }

        if (rolesService.getRoleByName(adminClientRoleName).isEmpty()) {
            log.info("Admin client role {} wasn't found and will be created", adminClientRoleName);

            final RoleBO adminClientRole = createRole(adminClientRoleName);
            log.info("Created admin client role {}", adminClientRole);
        }
    }

    private RoleBO createRole(final String roleName) {
        final RoleBO role = RoleBO.builder()
                .name(roleName)
                .build();

        return rolesService.create(role);
    }
}
