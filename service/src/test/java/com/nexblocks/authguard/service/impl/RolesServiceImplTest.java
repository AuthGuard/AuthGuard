package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.persistence.RolesRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.RoleBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RolesServiceImplTest {
    private RolesRepository rolesRepository;

    private RolesService rolesService;

    private static final String[] SKIPPED_FIELDS = new String[] { "id", "createdAt", "lastModified" };

    @BeforeEach
    void setup() {
        rolesRepository = Mockito.mock(RolesRepository.class);
        MessageBus messageBus = Mockito.mock(MessageBus.class);

        rolesService = new RolesServiceImpl(rolesRepository, new ServiceMapperImpl(), messageBus) ;
    }

    @Test
    void getAll() {
        List<RoleDO> roles = Arrays.asList(
                RoleDO.builder().name("role-1").build(),
                RoleDO.builder().name("role-2").build()
        );

        Mockito.when(rolesRepository.getAll("main")).thenReturn(CompletableFuture.completedFuture(roles));

        List<RoleBO> expected = Arrays.asList(
                RoleBO.builder().name("role-1").build(),
                RoleBO.builder().name("role-2").build()
        );

        List<RoleBO> actual = rolesService.getAll("main").join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void create() {
        RoleBO request = RoleBO.builder()
                .name("role")
                .domain("main")
                .build();

        Mockito.when(rolesRepository.getByName("role", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Mockito.when(rolesRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, RoleDO.class)));

        RoleBO actual = rolesService.create(request).join();

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(request);
    }

    @Test
    void createDuplicate() {
        RoleDO role = RoleDO.builder()
                .name("role")
                .domain("main")
                .build();

        RoleBO request = RoleBO.builder()
                .name("role")
                .domain("main")
                .build();

        Mockito.when(rolesRepository.getByName("role", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(role)));

        Mockito.when(rolesRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, RoleDO.class)));

        assertThatThrownBy(() -> rolesService.create(request).join())
                .hasCauseInstanceOf(ServiceConflictException.class);
    }

    @Test
    void getRoleByName() {
        RoleDO role = RoleDO.builder()
                .name("role")
                .build();

        Mockito.when(rolesRepository.getByName("role", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(role)));

        RoleBO expected = RoleBO.builder()
                .name("role")
                .build();

        Optional<RoleBO> actual = rolesService.getRoleByName("role", "main").join();

        assertThat(actual).contains(expected);
    }

    @Test
    void verifyRoles() {
        List<String> request = Arrays.asList("role-1", "role-2");

        Mockito.when(rolesRepository.getMultiple(request, "main"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                        RoleDO.builder().name("role-1").build(),
                        RoleDO.builder().name("role-2").build()
                )));

        List<String> actual = rolesService.verifyRoles(request, "main");

        assertThat(actual).isEqualTo(request);
    }

    @Test
    void verifyInvalidRoles() {
        List<String> request = Arrays.asList("role-1", "role-2");

        Mockito.when(rolesRepository.getMultiple(request, "main"))
                .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(
                        RoleDO.builder().name("role-1").build()
                )));

        List<String> expected = Collections.singletonList("role-1");

        List<String> actual = rolesService.verifyRoles(request, "main");

        assertThat(actual).isEqualTo(expected);
    }
}