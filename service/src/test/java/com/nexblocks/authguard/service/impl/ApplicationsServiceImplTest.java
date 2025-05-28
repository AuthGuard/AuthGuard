package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.AppDO;
import com.nexblocks.authguard.dal.persistence.ApplicationsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.*;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import io.smallrye.mutiny.Uni;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ApplicationsServiceImplTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 4));

    private ApplicationsRepository applicationsRepository;
    private ApplicationsService applicationsService;
    private AccountsService accountsService;
    private RolesService rolesService;
    private PermissionsService permissionsService;
    private IdempotencyService idempotencyService;
    private MessageBus messageBus;
    private ServiceMapper serviceMapper;

    private static final String[] SKIPPED_FIELDS = new String[] { "id", "createdAt", "lastModified", "entityType" };

    private AppDO createAppDO() {
        AppDO app = random.nextObject(AppDO.class);

        app.setDomain("main");

        return app;
    }
    
    @BeforeEach
    void setup() {
        applicationsRepository = Mockito.mock(ApplicationsRepository.class);
        accountsService = Mockito.mock(AccountsService.class);
        rolesService = Mockito.mock(RolesService.class);
        permissionsService = Mockito.mock(PermissionsService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        messageBus = Mockito.mock(MessageBus.class);

        serviceMapper = new ServiceMapperImpl();

        applicationsService = new ApplicationsServiceImpl(applicationsRepository, accountsService,
                idempotencyService, permissionsService, rolesService, serviceMapper, messageBus);
    }

    @Test
    void create() {
        AppBO app = random.nextObject(AppBO.class)
                .withDomain("main");

        String idempotentKey = "idempotent-key";
        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsService.getById(app.getParentAccountId(), "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(random.nextObject(AccountBO.class))));

        Mockito.when(applicationsRepository.save(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AppDO.class)));

        Mockito.when(idempotencyService.performOperationAsync(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(app.getEntityType())))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(rolesService.verifyRoles(app.getRoles(), "main", EntityType.APPLICATION))
                .thenReturn(Uni.createFrom().item(new ArrayList<>(app.getRoles())));

        Mockito.when(permissionsService.validate(app.getPermissions(), "main", EntityType.APPLICATION))
                .thenReturn(Uni.createFrom().item(new ArrayList<>(app.getPermissions())));

        AppBO created = applicationsService.create(app, requestContext).subscribeAsCompletionStage().join();
        List<PermissionBO> expectedPermissions = app.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(created)
                .usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(app.withPermissions(expectedPermissions));

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("apps"), any());
    }

    @Test
    void getById() {
        AppBO app = random.nextObject(AppBO.class)
                .withDomain("main")
                .withDeleted(false);

        Mockito.when(applicationsRepository.getById(Mockito.anyLong()))
                .thenReturn(Uni.createFrom().item(Optional.of(serviceMapper.toDO(app))));

        Optional<AppBO> retrieved = applicationsService.getById(1, "main").subscribeAsCompletionStage().join();
        List<PermissionBO> expectedPermissions = app.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).usingRecursiveComparison()
                .ignoringFields("permissions", "entityType")
                .isEqualTo(app.withPermissions(expectedPermissions));
    }

    @Test
    void delete() {
        AppDO app = createAppDO();

        app.setDeleted(false);

        Mockito.when(applicationsRepository.delete(app.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(app)));

        applicationsService.delete(app.getId(), "main");

        Mockito.verify(applicationsRepository).delete(app.getId());
    }

    @Test
    void activate() {
        AppDO app = createAppDO();

        app.setActive(false);
        app.setDomain("main");

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        AppBO updated = applicationsService.activate(app.getId(), "main").subscribeAsCompletionStage().join();

        assertThat(updated).isNotNull();
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void deactivate() {
        AppDO app = createAppDO();

        app.setActive(true);
        app.setDomain("main");

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        AppBO updated = applicationsService.deactivate(app.getId(), "main").subscribeAsCompletionStage().join();

        assertThat(updated).isNotNull();
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void grantPermissions() {
        AppDO account = createAppDO();

        Mockito.when(applicationsRepository.getById(account.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));
        Mockito.when(permissionsService.validate(any(), eq("main"), eq(EntityType.APPLICATION)))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, List.class)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        List<PermissionBO> permissions = Arrays.asList(
                random.nextObject(PermissionBO.class).withEntityType(null),
                random.nextObject(PermissionBO.class).withEntityType(null)
        );

        Optional<AppBO> updated = applicationsService.grantPermissions(account.getId(), permissions, "main").subscribeAsCompletionStage().join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(account);
        assertThat(updated.get().getPermissions()).contains(permissions.toArray(new PermissionBO[0]));
    }

    @Test
    void grantPermissionsInvalidPermission() {
        AppDO account = createAppDO();

        Mockito.when(applicationsRepository.getById(account.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(rolesService.verifyRoles(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        Mockito.when(permissionsService.validate(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Collections.emptyList()));

        List<PermissionBO> permissions = Arrays.asList(
                random.nextObject(PermissionBO.class),
                random.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> applicationsService.grantPermissions(account.getId(), permissions, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void grantPermissionsFromDifferentDomain() {
        AppDO account = createAppDO();

        Mockito.when(applicationsRepository.getById(account.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));
        Mockito.when(permissionsService.validate(any(), eq("other"), eq(EntityType.APPLICATION)))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, List.class)));
        Mockito.when(permissionsService.validate(any(), eq("main"), eq(EntityType.APPLICATION)))
                .thenAnswer(invocation -> Uni.createFrom().item(Collections.emptyList()));

        List<PermissionBO> permissions = Arrays.asList(
                random.nextObject(PermissionBO.class),
                random.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> applicationsService.grantPermissions(account.getId(), permissions, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void revokePermissions() {
        AppDO account = createAppDO();
        List<PermissionBO> currentPermissions = account.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getPermissionGroup())
                        .name(permissionDO.getName())
                        .build()
                ).toList();

        Mockito.when(applicationsRepository.getById(account.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        List<PermissionBO> permissionsToRevoke = Arrays.asList(
                currentPermissions.get(0),
                currentPermissions.get(1)
        );

        Optional<AppBO> updated = applicationsService.revokePermissions(account.getId(), permissionsToRevoke, "main").subscribeAsCompletionStage().join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(account);
        assertThat(updated.get().getPermissions()).doesNotContain(permissionsToRevoke.toArray(new PermissionBO[0]));
    }

    @Test
    void grantRoles() {
        AppDO app = createAppDO();

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        List<String> roles = Arrays.asList(
                random.nextObject(String.class),
                random.nextObject(String.class)
        );

        Mockito.when(rolesService.verifyRoles(roles, "main", EntityType.APPLICATION))
                .thenReturn(Uni.createFrom().item(roles));

        Optional<AppBO> updated = applicationsService.grantRoles(app.getId(), roles, "main").subscribeAsCompletionStage().join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(app);
        assertThat(updated.get().getRoles()).contains(roles.toArray(new String[0]));
    }

    @Test
    void grantRolesInvalidRoles() {
        AppDO app = createAppDO();

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        List<String> roles = Arrays.asList(
                random.nextObject(String.class),
                random.nextObject(String.class)
        );

        List<String> validRoles = Collections.singletonList(roles.get(0));

        Mockito.when(rolesService.verifyRoles(roles, "main", EntityType.APPLICATION))
                .thenReturn(Uni.createFrom().item(validRoles));

        assertThatThrownBy(() -> applicationsService.grantRoles(app.getId(), roles, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void grantRolesFromDifferentDomain() {
        AppDO app = createAppDO();

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        List<String> roles = Arrays.asList(
                random.nextObject(String.class),
                random.nextObject(String.class)
        );

        Mockito.when(rolesService.verifyRoles(roles, "other", EntityType.APPLICATION))
                .thenReturn(Uni.createFrom().item(roles));

        Mockito.when(rolesService.verifyRoles(roles, "main", EntityType.APPLICATION))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        Mockito.when(permissionsService.validate(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        assertThatThrownBy(() -> applicationsService.grantRoles(app.getId(), roles, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void revokeRoles() {
        AppDO app = createAppDO();
        List<String> currentRoles = new ArrayList<>(app.getRoles());

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.of(invocation.getArgument(0, AppDO.class))));

        List<String> rolesToRevoke = Arrays.asList(
                currentRoles.get(0),
                currentRoles.get(1)
        );

        Optional<AppBO> updated = applicationsService.revokeRoles(app.getId(), rolesToRevoke, "main").subscribeAsCompletionStage().join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(app);
        assertThat(updated.get().getRoles()).doesNotContain(rolesToRevoke.toArray(new String[0]));
    }
}