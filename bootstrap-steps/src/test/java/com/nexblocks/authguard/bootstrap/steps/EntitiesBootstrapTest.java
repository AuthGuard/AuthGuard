package com.nexblocks.authguard.bootstrap.steps;

import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.BootstrapEntitiesConfig;
import com.nexblocks.authguard.service.config.DomainEntitiesConfig;
import com.nexblocks.authguard.service.config.PermissionConfig;
import com.nexblocks.authguard.service.config.RolesConfig;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RoleBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class EntitiesBootstrapTest {

    private RolesService rolesService;
    private PermissionsService permissionsService;

    private EntitiesBootstrap entitiesBootstrap;

    @BeforeEach
    public void setup() {
        rolesService = Mockito.mock(RolesService.class);
        permissionsService = Mockito.mock(PermissionsService.class);

         BootstrapEntitiesConfig entitiesConfig = BootstrapEntitiesConfig.builder()
                .putDomains("test", DomainEntitiesConfig.builder()
                        .addRoles(RolesConfig.builder()
                                .name("user")
                                .build())
                        .addRoles(RolesConfig.builder()
                                .name("admin")
                                .build())
                        .addRoles(RolesConfig.builder()
                                .name("existing")
                                .build())
                        .addPermissions(PermissionConfig.builder()
                                .group("tests")
                                .name("existing")
                                .build())
                        .addPermissions(PermissionConfig.builder()
                                .group("tests")
                                .name("read")
                                .build())
                        .build())
                .putDomains("empty", DomainEntitiesConfig.builder()
                        .build())
                .build();

        entitiesBootstrap = new EntitiesBootstrap(rolesService, permissionsService, entitiesConfig);
    }

    @Test
    void run() {
        Mockito.when(rolesService.getRoleByName("existing", "test"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(RoleBO.builder()
                        .domain("test")
                        .name("existing")
                        .build())));

        Mockito.when(rolesService.getRoleByName(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Mockito.when(permissionsService.get("test", "tests", "existing"))
                        .thenReturn(CompletableFuture.completedFuture(Optional.of(
                                PermissionBO.builder()
                                        .group("tests")
                                        .name("existing")
                                        .domain("test")
                                        .build()
                        )));

        Mockito.when(permissionsService.get("test", "tests", "read"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Mockito.when(permissionsService.validate(Mockito.anyList(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Collections.singletonList(PermissionBO.builder()
                        .group("tests")
                        .name("existing")
                        .domain("test")
                        .build()));

        entitiesBootstrap.run();

        Mockito.verify(rolesService, Mockito.times(1))
                .create(RoleBO.builder()
                        .domain("test")
                        .name("user")
                        .build());

        Mockito.verify(rolesService, Mockito.times(1))
                .create(RoleBO.builder()
                        .domain("test")
                        .name("admin")
                        .build());

        Mockito.verify(permissionsService, Mockito.times(1))
                .create(PermissionBO.builder()
                        .domain("test")
                        .group("tests")
                        .name("read")
                        .build());
    }
}