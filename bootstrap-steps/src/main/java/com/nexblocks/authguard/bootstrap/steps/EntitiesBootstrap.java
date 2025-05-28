package com.nexblocks.authguard.bootstrap.steps;

import com.nexblocks.authguard.bootstrap.BootstrapStepResult;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniAndGroupIterable;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.BootstrapEntitiesConfig;
import com.nexblocks.authguard.service.config.PermissionConfig;
import com.nexblocks.authguard.service.config.RolesConfig;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RoleBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class EntitiesBootstrap implements BootstrapStep {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final RolesService rolesService;
    private final PermissionsService permissionsService;

    private final BootstrapEntitiesConfig entitiesConfig;

    @Inject
    public EntitiesBootstrap(final RolesService rolesService,
                             final PermissionsService permissionsService,
                             final @Named("entities") ConfigContext entitiesConfig) {
        this(rolesService, permissionsService, entitiesConfig == null
                ? null
                : entitiesConfig.asConfigBean(BootstrapEntitiesConfig.class));
    }

    public EntitiesBootstrap(final RolesService rolesService,
                             final PermissionsService permissionsService,
                             final BootstrapEntitiesConfig entitiesConfig) {
        this.rolesService = rolesService;
        this.permissionsService = permissionsService;
        this.entitiesConfig = entitiesConfig;
    }

    @Override
    public Uni<BootstrapStepResult> run() {
        if (entitiesConfig == null || entitiesConfig.getDomains() == null) {
            log.info("No entities config was provided. Skipping.");

            return Uni.createFrom().item(BootstrapStepResult.success());
        }

        // create roles
        List<Uni<BootstrapStepResult>> rolesUnis = entitiesConfig.getDomains().entrySet().stream()
                .map(entry -> {
                    String domain = entry.getKey();
                    List<RolesConfig> roles = entry.getValue().getRoles();

                    return createRoles(domain, roles);
                })
                .toList();

        List<Uni<BootstrapStepResult>> permissionsUnis = entitiesConfig.getDomains().entrySet().stream()
                .map(entry -> {
                    String domain = entry.getKey();
                    List<PermissionConfig> permissions = entry.getValue().getPermissions();

                    return createPermissions(domain, permissions);
                })
                .toList();

        List<Uni<BootstrapStepResult>> allUnis = Stream.concat(rolesUnis.stream(), permissionsUnis.stream()).toList();

        if (allUnis.isEmpty()) {
            return Uni.createFrom().item(BootstrapStepResult.success());
        }

        return Uni.combine().all()
                .unis(allUnis)
                .with(ignored -> BootstrapStepResult.success());
    }

    private Uni<BootstrapStepResult> createRoles(String domain, List<RolesConfig> roles) {
        if (roles == null) {
            return Uni.createFrom().item(BootstrapStepResult.success());
        }

        List<Uni<BootstrapStepResult>> rolesUnis = roles.stream()
                .map(rolesConfig -> rolesService.getRoleByName(rolesConfig.getName(), domain)
                        .flatMap(opt -> {
                            if (opt.isPresent()) {
                                log.info("Role {} already exists in domain {}", opt.get().getName(), domain);

                                return Uni.createFrom().item(BootstrapStepResult.success());
                            }

                            RoleBO role = RoleBO.builder()
                                    .domain(domain)
                                    .name(rolesConfig.getName())
                                    .forAccounts(rolesConfig.isForAccounts())
                                    .forApplications(rolesConfig.isForApplications())
                                    .build();

                            return rolesService.create(role)
                                    .map(created -> {
                                        log.info("Created role {} in domain {}", role.getName(), domain);

                                        return BootstrapStepResult.success();
                                    });

                        }))
                .toList();

        if (rolesUnis.isEmpty()) {
            return Uni.createFrom().item(BootstrapStepResult.success());
        }

        // 2.  Combine them into a single Uni<Result>
        return Uni.combine().all().unis(rolesUnis)
                .with(list -> BootstrapStepResult.success()); // list is List<RoleBO>

    }

    private Uni<BootstrapStepResult> createPermissions(String domain, List<PermissionConfig> permissions) {
        if (permissions == null) {
            return null;
        }

        List<PermissionBO> permissionBOS = permissions.stream()
                .map(permission -> PermissionBO.builder()
                        .group(permission.getGroup())
                        .name(permission.getName())
                        .forAccounts(permission.isForAccounts())
                        .forApplications(permission.isForApplications())
                        .build())
                .toList();

        List<Uni<BootstrapStepResult>> unis = permissionBOS.stream()
                .map(permission -> permissionsService.get(domain, permission.getGroup(), permission.getName())
                        .flatMap(opt -> {
                            if (opt.isPresent()) {
                                return Uni.createFrom().item(BootstrapStepResult.success());
                            }

                            return permissionsService.create(permission.withDomain(domain))
                                    .map(created -> {
                                        log.info("Created permission {}:{} in domain {}", permission.getGroup(), permission.getName(), domain);

                                        return BootstrapStepResult.success();
                                    });
                        }))
                .toList();

        if (unis.isEmpty()) {
            return Uni.createFrom().item(BootstrapStepResult.success());
        }

        return Uni.combine().all().unis(unis)
                .with(ignored -> BootstrapStepResult.success());
    }
}
