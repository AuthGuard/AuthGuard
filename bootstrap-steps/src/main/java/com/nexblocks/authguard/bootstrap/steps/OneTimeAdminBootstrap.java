package com.nexblocks.authguard.bootstrap.steps;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OneTimeAdminBootstrap implements BootstrapStep {
    private static final String OTA_ROLE = "one_time_admin";
    private static final String RESERVED_DOMAIN = "global";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AccountsService accountsService;
    private final RolesService rolesService;
    private final ConfigContext oneTimeAdminConfig;

    @Inject
    public OneTimeAdminBootstrap(final AccountsService accountsService,
                                 final RolesService rolesService,
                                 @Named("oneTimeAdmin") final ConfigContext oneTimeAdminConfig) {
        this.accountsService = accountsService;
        this.rolesService = rolesService;
        this.oneTimeAdminConfig = oneTimeAdminConfig;
    }

    @Override
    public void run() {
        final List<AccountBO> admins = accountsService.getAdmins().join();
        final List<AccountBO> oneTimeAdmins = accountsService.getByRole(OTA_ROLE, RESERVED_DOMAIN).join();

        if (admins.isEmpty() && oneTimeAdmins.isEmpty()) {
            log.info("No admin accounts were found, a one-time admin account will be created");

            if (rolesService.getRoleByName(OTA_ROLE, RESERVED_DOMAIN).isEmpty()) {
                log.info("Default role {} wasn't found and will be created", OTA_ROLE);

                final RoleBO created = createRole(OTA_ROLE);
                log.info("Created default role {}", created);
            }

            final RequestContextBO requestContext = RequestContextBO.builder()
                    .idempotentKey(UUID.randomUUID().toString())
                    .build();

            final AccountBO createdAccount = accountsService.create(oneTimeAccount(), requestContext).join();

            log.info("A one-time admin account was created with {}", createdAccount.getIdentifiers());
        }
    }

    private AccountBO oneTimeAccount() {
        final String otaUsernameEnvVariable = oneTimeAdminConfig.getAsString("usernameVariable");
        final String otaPasswordEnvVariable = oneTimeAdminConfig.getAsString("passwordVariable");

        if (otaUsernameEnvVariable == null || otaPasswordEnvVariable == null) {
            throw new ConfigurationException("Missing either 'usernameVariable' or 'passwordVariable' in one-time admin configuration");
        }

        final String username = System.getenv(otaUsernameEnvVariable);
        final String password = System.getenv(otaPasswordEnvVariable);

        if (username == null) {
            throw new ConfigurationException("No value was provided for " + otaUsernameEnvVariable);
        }

        if (password == null) {
            throw new ConfigurationException("No value was provided for " + otaPasswordEnvVariable);
        }

        return AccountBO.builder()
                .roles(Collections.singletonList(OTA_ROLE))
                .active(true)
                .domain(RESERVED_DOMAIN)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(username)
                        .type(UserIdentifier.Type.USERNAME)
                        .domain(RESERVED_DOMAIN)
                        .build())
                .plainPassword(password)
                .build();
    }

    private RoleBO createRole(final String roleName) {
        final RoleBO role = RoleBO.builder()
                .name(roleName)
                .domain(RESERVED_DOMAIN)
                .build();

        return rolesService.create(role).join();
    }

}
