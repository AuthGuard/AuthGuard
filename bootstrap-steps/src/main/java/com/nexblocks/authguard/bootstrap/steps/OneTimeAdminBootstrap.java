package com.nexblocks.authguard.bootstrap.steps;

import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.CredentialsService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OneTimeAdminBootstrap implements BootstrapStep {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AccountsService accountsService;
    private final CredentialsService credentialsService;
    private final ConfigContext oneTimeAdminConfig;

    @Inject
    public OneTimeAdminBootstrap(final AccountsService accountsService,
                                 final CredentialsService credentialsService,
                                 @Named("oneTimeAdmin") final ConfigContext oneTimeAdminConfig) {
        this.accountsService = accountsService;
        this.credentialsService = credentialsService;
        this.oneTimeAdminConfig = oneTimeAdminConfig;
    }

    @Override
    public void run() {
        final List<AccountBO> admins = accountsService.getAdmins();

        if (admins.isEmpty()) {
            log.info("No admin accounts were found, a one-time admin account will be created");
            final RequestContextBO requestContext = RequestContextBO.builder()
                    .idempotentKey(UUID.randomUUID().toString())
                    .build();

            final AccountBO createdAccount = accountsService.create(oneTimeAccount(), requestContext);
            final CredentialsBO createdCredentials = credentialsService
                    .create(oneTimeAdminCredentials(createdAccount.getId()), requestContext);

            log.info("A one-time admin account was created with {}", createdCredentials.getIdentifiers());
        }
    }

    private AccountBO oneTimeAccount() {
        return AccountBO.builder()
                .roles(Collections.singletonList("one_time_admin"))
                .active(true)
                .build();
    }

    private CredentialsBO oneTimeAdminCredentials(final String accountId) {
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

        return CredentialsBO.builder()
                .accountId(accountId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(username)
                        .type(UserIdentifier.Type.USERNAME)
                        .build())
                .plainPassword(password)
                .build();
    }
}
