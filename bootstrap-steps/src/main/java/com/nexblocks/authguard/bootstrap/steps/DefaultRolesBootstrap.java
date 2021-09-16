package com.nexblocks.authguard.bootstrap.steps;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.model.RoleBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class DefaultRolesBootstrap implements BootstrapStep {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final RolesService rolesService;
    private final AccountConfig accountConfig;

    @Inject
    public DefaultRolesBootstrap(final RolesService rolesService,
                                 final @Named("accounts") ConfigContext accountsConfig) {
        this.rolesService = rolesService;
        this.accountConfig = accountsConfig.asConfigBean(AccountConfig.class);
    }

    @Override
    public void run() {
        final Set<String> defaultRoles = accountConfig.getDefaultRoles();

        defaultRoles.forEach(role -> {
            if (rolesService.getRoleByName(role).isEmpty()) {
                log.info("Default role {} wasn't found and will be created", role);

                final RoleBO created = createRole(role);
                log.info("Created default role {}", created);
            }
        });
    }

    private RoleBO createRole(final String roleName) {
        final RoleBO role = RoleBO.builder()
                .name(roleName)
                .build();

        return rolesService.create(role);
    }
}
