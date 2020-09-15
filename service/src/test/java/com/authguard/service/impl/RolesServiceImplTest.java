package com.authguard.service.impl;

import com.authguard.dal.RolesRepository;
import com.authguard.dal.model.RoleDO;
import com.authguard.service.PermissionsService;
import com.authguard.service.RolesService;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.RoleBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RolesServiceImplTest {
    private RolesRepository rolesRepository;
    private PermissionsService permissionsService;

    private RolesService rolesService;

    @BeforeEach
    void setup() {
        rolesRepository = Mockito.mock(RolesRepository.class);
        permissionsService = Mockito.mock(PermissionsService.class);

        rolesService = new RolesServiceImpl(rolesRepository, permissionsService, new ServiceMapperImpl()) ;
    }

    @Test
    void getAll() {
        final List<RoleDO> roles = Arrays.asList(
                RoleDO.builder().name("role-1").permissions(Collections.emptyList()).build(),
                RoleDO.builder().name("role-2").permissions(Collections.emptyList()).build()
        );

        Mockito.when(rolesRepository.getAll()).thenReturn(CompletableFuture.completedFuture(roles));

        final List<RoleBO> expected = Arrays.asList(
                RoleBO.builder().name("role-1").build(),
                RoleBO.builder().name("role-2").build()
        );

        final List<RoleBO> actual = rolesService.getAll();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void create() {
        final RoleBO request = RoleBO.builder()
                .name("role")
                .addPermissions(PermissionBO.builder()
                        .name("permission")
                        .group("group")
                        .build())
                .build();

        Mockito.when(rolesRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, RoleDO.class)));

        final RoleBO actual = rolesService.create(request);

        assertThat(actual).isEqualTo(request);
    }

    @Test
    void getRoleByName() {
        final RoleDO role = RoleDO.builder()
                .name("role")
                .permissions(Collections.emptyList())
                .build();

        Mockito.when(rolesRepository.getByName("role"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(role)));

        final RoleBO expected = RoleBO.builder()
                .name("role")
                .build();

        final Optional<RoleBO> actual = rolesService.getRoleByName("role");

        assertThat(actual).contains(expected);
    }

    @Test
    void verifyRoles() {
        final List<String> request = Arrays.asList("role-1", "role-2");

        Mockito.when(rolesRepository.getMultiple(request))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                        RoleDO.builder().name("role-1").permissions(Collections.emptyList()).build(),
                        RoleDO.builder().name("role-2").permissions(Collections.emptyList()).build()
                )));

        final List<String> actual = rolesService.verifyRoles(request);

        assertThat(actual).isEqualTo(request);
    }

    @Test
    void verifyInvalidRoles() {
        final List<String> request = Arrays.asList("role-1", "role-2");

        Mockito.when(rolesRepository.getMultiple(request))
                .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(
                        RoleDO.builder().name("role-1").permissions(Collections.emptyList()).build()
                )));

        final List<String> expected = Collections.singletonList("role-1");

        final List<String> actual = rolesService.verifyRoles(request);

        assertThat(actual).isEqualTo(expected);
    }
}