package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.persistence.PermissionsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.PermissionBO;
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
    private MessageBus messageBus;

    private PermissionsService permissionsService;

    @BeforeEach
    void setup() {
        permissionsRepository = Mockito.mock(PermissionsRepository.class);
        messageBus = Mockito.mock(MessageBus.class);

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

        assertThat(actual).isEqualToIgnoringGivenFields(request, "id", "createdAt", "lastModified");
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

        Optional<PermissionBO> actual = permissionsService.getById(permission.getId()).join();

        assertThat(actual).contains(expected);
    }

    @Test
    void update() {
        assertThatThrownBy(() -> permissionsService.update(PermissionBO.builder().build()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getAll() {
        List<PermissionDO> permissions = Arrays.asList(
                PermissionDO.builder().group("test").name("read").build(),
                PermissionDO.builder().group("test").name("write").build()
        );

        Mockito.when(permissionsRepository.getAll("main"))
                .thenReturn(CompletableFuture.completedFuture(permissions));

        List<PermissionBO> expected = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build()
        );

        List<PermissionBO> actual = permissionsService.getAll("main");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getAllForGroup() {
        List<PermissionDO> permissions = Arrays.asList(
                PermissionDO.builder().group("test").name("read").build(),
                PermissionDO.builder().group("test").name("write").build()
        );

        Mockito.when(permissionsRepository.getAllForGroup("test", "main"))
                .thenReturn(CompletableFuture.completedFuture(permissions));

        List<PermissionBO> expected = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build()
        );

        List<PermissionBO> actual = permissionsService.getAllForGroup("test", "main");

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

        Optional<PermissionBO> actual = permissionsService.delete(permission.getId()).join();

        assertThat(actual).contains(expected);
    }

    @Test
    void verifyPermissions() {
        List<PermissionDO> existing = Collections.singletonList(
                PermissionDO.builder().group("test").name("read").build()
        );

        List<PermissionBO> request = Arrays.asList(
                PermissionBO.builder().group("test").name("read").build(),
                PermissionBO.builder().group("test").name("write").build()
        );

        Mockito.when(permissionsRepository.search("test", "read", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing.get(0))));

        Mockito.when(permissionsRepository.search("test", "write", "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        List<PermissionBO> expected = Collections.singletonList(
                PermissionBO.builder().group("test").name("read").build()
        );

        List<PermissionBO> actual = permissionsService.validate(request, "main");

        assertThat(actual).isEqualTo(expected);
    }

}