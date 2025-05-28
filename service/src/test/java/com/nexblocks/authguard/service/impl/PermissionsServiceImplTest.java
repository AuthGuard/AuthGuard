package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.persistence.LongPage;
import com.nexblocks.authguard.dal.persistence.PermissionsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.PermissionBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

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
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        Mockito.when(permissionsRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, PermissionDO.class)));

        PermissionBO actual = permissionsService.create(request).subscribeAsCompletionStage().join();

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
                .thenReturn(Uni.createFrom().item(Optional.of(permission)));

        Mockito.when(permissionsRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, PermissionDO.class)));

        assertThatThrownBy(() -> permissionsService.create(request)).isInstanceOf(ServiceConflictException.class);
    }

    @Test
    void getById() {
        PermissionDO permission = PermissionDO.builder()
                .id(1)
                .permissionGroup("test")
                .name("read")
                .build();

        PermissionBO expected = PermissionBO.builder()
                .id(1)
                .group("test")
                .name("read")
                .build();

        Mockito.when(permissionsRepository.getById(permission.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(permission)));

        Optional<PermissionBO> actual = permissionsService.getById(permission.getId(), "main").subscribeAsCompletionStage().join();

        assertThat(actual).contains(expected);
    }

    @Test
    void getAll() {
        List<PermissionDO> permissions = Arrays.asList(
                PermissionDO.builder().permissionGroup("test").name("read").build(),
                PermissionDO.builder().permissionGroup("test").name("write").build()
        );

        Mockito.when(permissionsRepository.getAll("main", LongPage.of(null, 20)))
                .thenReturn(Uni.createFrom().item(permissions));

        List<PermissionBO> expected = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build()
        );

        List<PermissionBO> actual = permissionsService.getAll("main", null).subscribeAsCompletionStage().join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getAllForGroup() {
        List<PermissionDO> permissions = Arrays.asList(
                PermissionDO.builder().permissionGroup("test").name("read").build(),
                PermissionDO.builder().permissionGroup("test").name("write").build()
        );

        Mockito.when(permissionsRepository.getAllForGroup("test", "main", LongPage.of(null, 20)))
                .thenReturn(Uni.createFrom().item(permissions));

        List<PermissionBO> expected = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build()
        );

        List<PermissionBO> actual = permissionsService.getAllForGroup("test", "main", null).subscribeAsCompletionStage().join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void delete() {
        PermissionDO permission = PermissionDO.builder()
                .id(1)
                .permissionGroup("test")
                .name("read")
                .build();

        PermissionBO expected = PermissionBO.builder()
                .id(1)
                .group("test")
                .name("read")
                .build();

        Mockito.when(permissionsRepository.delete(permission.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(permission)));

        Optional<PermissionBO> actual = permissionsService.delete(permission.getId(), "main").subscribeAsCompletionStage().join();

        assertThat(actual).contains(expected);
    }

    @Test
    void verifyPermissionsForAccount() {
        List<PermissionDO> existing = Arrays.asList(
                PermissionDO.builder().permissionGroup("test").name("read").forAccounts(true).build(),
                PermissionDO.builder().permissionGroup("test").name("write").forAccounts(false).build()
        );

        List<PermissionBO> request = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build(),
                PermissionBO.builder().group("test").name("delete").build()
        );

        Mockito.when(permissionsRepository.search("test", "read", "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(existing.get(0))));

        Mockito.when(permissionsRepository.search("test", "write", "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(existing.get(1))));

        Mockito.when(permissionsRepository.search("test", "delete", "main"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        List<PermissionBO> expected = Collections.singletonList(
                PermissionBO.builder().group("test").name("read").forAccounts(true).build()
        );

        List<PermissionBO> actual = permissionsService.validate(request, "main", EntityType.ACCOUNT).subscribeAsCompletionStage().join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void verifyPermissionsForApplication() {
        List<PermissionDO> existing = Arrays.asList(
                PermissionDO.builder().permissionGroup("test").name("read").forApplications(false).build(),
                PermissionDO.builder().permissionGroup("test").name("write").forApplications(true).build()
        );

        List<PermissionBO> request = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build(),
                PermissionBO.builder().group("test").name("delete").build()
        );

        Mockito.when(permissionsRepository.search("test", "read", "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(existing.get(0))));

        Mockito.when(permissionsRepository.search("test", "write", "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(existing.get(1))));

        Mockito.when(permissionsRepository.search("test", "delete", "main"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        List<PermissionBO> expected = Collections.singletonList(
                PermissionBO.builder().group("test").name("write").forApplications(true).build()
        );

        List<PermissionBO> actual = permissionsService.validate(request, "main", EntityType.APPLICATION).subscribeAsCompletionStage().join();

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
                .thenReturn(Uni.createFrom().item(
                        Optional.of(PermissionDO.builder()
                                .id(1)
                                .domain("main")
                                .permissionGroup("tests")
                                .name("test")
                                .forAccounts(false)
                                .forApplications(true)
                                .build())));

        Mockito.when(permissionsRepository.update(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, PermissionDO.class))));

        PermissionBO actual = permissionsService.update(request, "main").subscribeAsCompletionStage().join().get();

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