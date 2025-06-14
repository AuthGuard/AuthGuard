package com.nexblocks.authguard.bootstrap.steps;

import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.bootstrap.BootstrapStepResult;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuples;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
    public Uni<BootstrapStepResult> run() {
        return accountsService.getAdmins()
                .flatMap(admins -> {
                    return accountsService.getByRole(OTA_ROLE, RESERVED_DOMAIN)
                            .map(oneTimeAdmins -> Tuple2.of(admins, oneTimeAdmins));
                })
                .flatMap(tuple -> {
                    List<AccountBO> admins = tuple.getItem1();
                    List<AccountBO> oneTimeAdmins = tuple.getItem2();

                    if (admins.isEmpty() && oneTimeAdmins.isEmpty()) {
                        log.info("No admin accounts were found, a one-time admin account will be created");

                        return createOneTimeAdmin().map(createdAccount -> {
                            log.info("A one-time admin account was created with {}", createdAccount.getIdentifiers());

                            return BootstrapStepResult.success();
                        });
                    }

                    return Uni.createFrom().item(BootstrapStepResult.success());
                });
    }

    private Uni<AccountBO> createOneTimeAdmin() {
        return rolesService.getRoleByName(OTA_ROLE, RESERVED_DOMAIN)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        log.info("Default role {} wasn't found and will be created", OTA_ROLE);

                        return createOtaRole();
                    }

                    return Uni.createFrom().item(opt.get());
                })
                .flatMap(role -> {
                    final RequestContextBO requestContext = RequestContextBO.builder()
                            .idempotentKey(UUID.randomUUID().toString())
                            .build();

                    return accountsService.create(oneTimeAccount(), requestContext);
                });
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

    private Uni<RoleBO> createOtaRole() {
        final RoleBO role = RoleBO.builder()
                .name(OneTimeAdminBootstrap.OTA_ROLE)
                .domain(RESERVED_DOMAIN)
                .forAccounts(true)
                .build();

        return rolesService.create(role);
    }

}
