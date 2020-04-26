package com.authguard.bootstrap.steps;

import com.authguard.bootstrap.BootstrapStep;
import com.authguard.config.ConfigContext;
import com.authguard.service.AccountsService;
import com.authguard.service.CredentialsService;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.CredentialsBO;
import com.authguard.service.model.UserIdentifier;
import com.authguard.service.model.UserIdentifierBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

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

            final AccountBO createdAccount = accountsService.create(oneTimeAccount());
            final CredentialsBO createdCredentials = credentialsService
                    .create(oneTimeAdminCredentials(createdAccount.getId()));

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
        final String username = System.getenv(oneTimeAdminConfig.getAsString("usernameVariable"));
        final String password = System.getenv(oneTimeAdminConfig.getAsString("passwordVariable"));

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
