package com.nexblocks.authguard.bootstrap.steps;

import com.nexblocks.authguard.bootstrap.BootstrapStepResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.model.RoleBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
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
    public Uni<BootstrapStepResult> run() {
        if (accountConfig == null) {
            log.info("No account config was provided. Skipping.");
            return Uni.createFrom().item(BootstrapStepResult.success());
        }

        final Map<String, Set<String>> defaultRolesByDomain = accountConfig.getDefaultRolesByDomain();

        if (defaultRolesByDomain == null || defaultRolesByDomain.isEmpty()) {
            log.info("No default roles were found");
            return Uni.createFrom().item(BootstrapStepResult.success());
        }

        // 1.  Build a Uni<RoleBO> for each (domain, role) combination
        List<Uni<BootstrapStepResult>> roleCreations = defaultRolesByDomain.entrySet().stream()
                .flatMap(e -> e.getValue()                     // Set<String> roles
                        .stream()
                        .map(role -> createRole(e.getKey(), role)
                                .map(created -> {
                                    log.info("Created default role {}", created);
                                    return BootstrapStepResult.success();
                                })))
                .toList();                                     // Java 17+; otherwise collect(Collectors.toList())

        // 2.  Combine them into a single Uni<Result>
        return Uni.combine().all().unis(roleCreations)
                .with(list -> BootstrapStepResult.success()); // list is List<RoleBO>
    }

    private Uni<RoleBO> createRole(final String domain, final String roleName) {
        RoleBO role = RoleBO.builder()
                .name(roleName)
                .domain(domain)
                .forAccounts(true)
                .forApplications(false)
                .build();

        return rolesService.create(role);
    }
}
