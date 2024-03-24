package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.persistence.Page;
import com.nexblocks.authguard.dal.persistence.PermissionsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.PermissionBO;
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

class PermissionsServiceImplTest {
    private PermissionsRepository permissionsRepository;

    private PermissionsService permissionsService;

    private static final String[] SKIPPED_FIELDS = { "id", "createdAt", "lastModified" };

    @BeforeEach
    void setup() {
        permissionsRepository = Mockito.mock(PermissionsRepository.class);
        MessageBus messageBus = Mockito.mock(MessageBus.class);

        permissionsService = new PermissionsServiceImpl(permissionsRepository, new ServiceMapperImpl(), messageBus) ;
    }

    @Test
    void create() {
        PermissionBO request = PermissionBO.builder()
                .group("test")
                .name("read")
                .domain("main")
                .build();

        Mockito.when(permissionsRepository.search(request.getGroup(), request.getName(), "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Mockito.when(permissionsRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, PermissionDO.class)));

        PermissionBO actual = permissionsService.create(request).join();

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(request);
    }

    @Test
    void createDuplicate() {
        PermissionDO permission = PermissionDO.builder()
                .build();

        PermissionBO request = PermissionBO.builder()
                .group("test")
                .name("read")
                .domain("main")
                .build();

        Mockito.when(permissionsRepository.search(request.getGroup(), request.getName(), "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(permission)));

        Mockito.when(permissionsRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, PermissionDO.class)));

        assertThatThrownBy(() -> permissionsService.create(request)).isInstanceOf(ServiceConflictException.class);
    }

    @Test
    void getById() {
        PermissionDO permission = PermissionDO.builder()
                .id(1)
                .group("test")
                .name("read")
                .build();

        PermissionBO expected = PermissionBO.builder()
                .id(1)
                .group("test")
                .name("read")
                .build();

        Mockito.when(permissionsRepository.getById(permission.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(permission)));

        Optional<PermissionBO> actual = permissionsService.getById(permission.getId(), "main").join();

        assertThat(actual).contains(expected);
    }

    @Test
    void getAll() {
        List<PermissionDO> permissions = Arrays.asList(
                PermissionDO.builder().group("test").name("read").build(),
                PermissionDO.builder().group("test").name("write").build()
        );

        Mockito.when(permissionsRepository.getAll("main", Page.of(null, 20)))
                .thenReturn(CompletableFuture.completedFuture(permissions));

        List<PermissionBO> expected = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build()
        );

        List<PermissionBO> actual = permissionsService.getAll("main", null).join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getAllForGroup() {
        List<PermissionDO> permissions = Arrays.asList(
                PermissionDO.builder().group("test").name("read").build(),
                PermissionDO.builder().group("test").name("write").build()
        );

        Mockito.when(permissionsRepository.getAllForGroup("test", "main", Page.of(null, 20)))
                .thenReturn(CompletableFuture.completedFuture(permissions));

        List<PermissionBO> expected = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build()
        );

        List<PermissionBO> actual = permissionsService.getAllForGroup("test", "main", null).join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void delete() {
        PermissionDO permission = PermissionDO.builder()
                .id(1)
                .group("test")
                .name("read")
                .build();

        PermissionBO expected = PermissionBO.builder()
                .id(1)
                .group("test")
                .name("read")
                .build();

        Mockito.when(permissionsRepository.delete(permission.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(permission)));

        Optional<PermissionBO> actual = permissionsService.delete(permission.getId(), "main").join();

        assertThat(actual).contains(expected);
    }

    @Test
    void verifyPermissionsForAccount() {
        List<PermissionDO> existing = Arrays.asList(
                PermissionDO.builder().group("test").name("read").forAccounts(true).build(),
                PermissionDO.builder().group("test").name("write").forAccounts(false).build()
        );

        List<PermissionBO> request = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build(),
                PermissionBO.builder().group("test").name("delete").build()
        );

        Mockito.when(permissionsRepository.search("test", "read", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing.get(0))));

        Mockito.when(permissionsRepository.search("test", "write", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing.get(1))));

        Mockito.when(permissionsRepository.search("test", "delete", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        List<PermissionBO> expected = Collections.singletonList(
                PermissionBO.builder().group("test").name("read").forAccounts(true).build()
        );

        List<PermissionBO> actual = permissionsService.validate(request, "main", EntityType.ACCOUNT);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void verifyPermissionsForApplication() {
        List<PermissionDO> existing = Arrays.asList(
                PermissionDO.builder().group("test").name("read").forApplications(false).build(),
                PermissionDO.builder().group("test").name("write").forApplications(true).build()
        );

        List<PermissionBO> request = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build(),
                PermissionBO.builder().group("test").name("delete").build()
        );

        Mockito.when(permissionsRepository.search("test", "read", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing.get(0))));

        Mockito.when(permissionsRepository.search("test", "write", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing.get(1))));

        Mockito.when(permissionsRepository.search("test", "delete", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        List<PermissionBO> expected = Collections.singletonList(
                PermissionBO.builder().group("test").name("write").forApplications(true).build()
        );

        List<PermissionBO> actual = permissionsService.validate(request, "main", EntityType.APPLICATION);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void updatePermission() {
        PermissionBO request = PermissionBO.builder()
                .id(1)
                .forAccounts(true)
                .forApplications(false)
                .build();

        Mockito.when(permissionsRepository.getById(1))
                .thenReturn(CompletableFuture.completedFuture(
                        Optional.of(PermissionDO.builder()
                                .id(1)
                                .domain("main")
                                .group("tests")
                                .name("test")
                                .forAccounts(false)
                                .forApplications(true)
                                .build())));

        Mockito.when(permissionsRepository.update(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, PermissionDO.class))));

        PermissionBO actual = permissionsService.update(request, "main").join().get();

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(PermissionBO.builder()
                        .id(1)
                        .domain("main")
                        .group("tests")
                        .name("test")
                        .forAccounts(true)
                        .forApplications(false)
                        .build());
    }
}