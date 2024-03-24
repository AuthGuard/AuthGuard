package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.persistence.Page;
import com.nexblocks.authguard.dal.persistence.RolesRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.EntityType;
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

        Mockito.when(rolesRepository.getAll("main", Page.of(null, 20))).thenReturn(CompletableFuture.completedFuture(roles));

        List<RoleBO> expected = Arrays.asList(
                RoleBO.builder().name("role-1").build(),
                RoleBO.builder().name("role-2").build()
        );

        List<RoleBO> actual = rolesService.getAll("main", null).join();

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
    void verifyRolesForAccount() {
        List<String> request = Arrays.asList("role-1", "role-2");

        Mockito.when(rolesRepository.getMultiple(request, "main"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                        RoleDO.builder().name("role-1").forAccounts(true).build(),
                        RoleDO.builder().name("role-2").forAccounts(false).build()
                )));

        List<String> actual = rolesService.verifyRoles(request, "main", EntityType.ACCOUNT);

        assertThat(actual).isEqualTo(Collections.singletonList("role-1"));
    }

    @Test
    void verifyRolesForApplication() {
        List<String> request = Arrays.asList("role-1", "role-2");

        Mockito.when(rolesRepository.getMultiple(request, "main"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                        RoleDO.builder().name("role-1").forApplications(true).build(),
                        RoleDO.builder().name("role-2").forApplications(false).build()
                )));

        List<String> actual = rolesService.verifyRoles(request, "main", EntityType.APPLICATION);

        assertThat(actual).isEqualTo(Collections.singletonList("role-1"));
    }

    @Test
    void verifyInvalidRoles() {
        List<String> request = Arrays.asList("role-1", "role-2");

        Mockito.when(rolesRepository.getMultiple(request, "main"))
                .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(
                        RoleDO.builder().name("role-1").forAccounts(true).build()
                )));

        List<String> expected = Collections.singletonList("role-1");

        List<String> actual = rolesService.verifyRoles(request, "main", EntityType.ACCOUNT);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void updateRole() {
        RoleBO request = RoleBO.builder()
                .id(1)
                .forAccounts(true)
                .forApplications(false)
                .build();

        Mockito.when(rolesRepository.getById(1))
                .thenReturn(CompletableFuture.completedFuture(
                        Optional.of(RoleDO.builder()
                                .id(1)
                                .domain("main")
                                .name("test")
                                .forAccounts(false)
                                .forApplications(true)
                                .build())));

        Mockito.when(rolesRepository.update(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, RoleDO.class))));

        RoleBO actual = rolesService.update(request, "main").join().get();

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(RoleBO.builder()
                        .id(1)
                        .domain("main")
                        .name("test")
                        .forAccounts(true)
                        .forApplications(false)
                        .build());
    }
}