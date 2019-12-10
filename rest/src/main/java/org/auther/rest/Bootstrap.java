package org.auther.rest;

import com.auther.config.ConfigContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.auther.service.AccountsService;
import org.auther.service.CredentialsService;
import org.auther.service.model.AccountBO;
import org.auther.service.model.CredentialsBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    private final AccountsService accountsService;
    private final CredentialsService credentialsService;
    private final ConfigContext oneTimeAdminConfig;

    @Inject
    public Bootstrap(final AccountsService accountsService, final CredentialsService credentialsService,
                     @Named("oneTimeAdmin") final ConfigContext oneTimeAdminConfig) {
        this.accountsService = accountsService;
        this.credentialsService = credentialsService;
        this.oneTimeAdminConfig = oneTimeAdminConfig;
    }

    public void bootstrapOneTimeAdmin() {
        final List<AccountBO> admins = accountsService.getAdmins();

        if (admins.isEmpty()) {
            LOG.info("No admin accounts were found, a one-time admin account will be created");

            final AccountBO createdAccount = accountsService.create(oneTimeAccount());
            final CredentialsBO createdCredentials = credentialsService
                    .create(oneTimeAdminCredentials(createdAccount.getId()));

            LOG.info("A one-time admin account was created with username {}", createdCredentials.getUsername());
        }
    }

    private AccountBO oneTimeAccount() {
        return AccountBO.builder()
                .roles(Collections.singletonList("one-time-admin"))
                .active(true)
                .build();
    }

    private CredentialsBO oneTimeAdminCredentials(final String accountId) {
        final String username = System.getenv(oneTimeAdminConfig.getAsString("usernameVariable"));
        final String password = System.getenv(oneTimeAdminConfig.getAsString("passwordVariable"));

        return CredentialsBO.builder()
                .accountId(accountId)
                .username(username)
                .plainPassword(password)
                .build();
    }
}
