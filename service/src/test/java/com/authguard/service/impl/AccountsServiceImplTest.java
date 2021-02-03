package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.dal.persistence.AccountsRepository;
import com.authguard.dal.model.AccountDO;
import com.authguard.emb.MessageBus;
import com.authguard.service.IdempotencyService;
import com.authguard.service.PermissionsService;
import com.authguard.service.RolesService;
import com.authguard.service.config.AccountConfig;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.RequestContextBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsServiceImplTest {
    private AccountsRepository accountsRepository;
    private PermissionsService permissionsService;
    private IdempotencyService idempotencyService;
    private RolesService rolesService;
    private MessageBus messageBus;
    private AccountsServiceImpl accountService;
    private ServiceMapper serviceMapper;

    private final static EasyRandom RANDOM = new EasyRandom(
            new EasyRandomParameters().collectionSizeRange(3, 5)
    );

    @BeforeAll
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        permissionsService = Mockito.mock(PermissionsService.class);
        rolesService = Mockito.mock(RolesService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        messageBus = Mockito.mock(MessageBus.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        final AccountConfig accountConfig = AccountConfig.builder()
                .verifyEmail(true)
                .build();

        Mockito.when(configContext.asConfigBean(AccountConfig.class))
                .thenReturn(accountConfig);

        serviceMapper = new ServiceMapperImpl();
        accountService = new AccountsServiceImpl(accountsRepository, permissionsService,rolesService,
                idempotencyService, serviceMapper, messageBus, configContext);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(accountsRepository);
        Mockito.reset(permissionsService);
        Mockito.reset(messageBus);
    }

    @Test
    void create() {
        final AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withDeleted(false)
                .withId(null);

        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountDO.class)));

        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(account.getEntityType())))
                .thenAnswer(invocation -> {
                    return CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get());
                });

        final AccountBO persisted = accountService.create(account, requestContext);
        final List<PermissionBO> expectedPermissions = account.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(persisted).isNotNull();
        assertThat(persisted)
                .isEqualToIgnoringGivenFields(account.withPermissions(expectedPermissions), "id");

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus).publish(eq("verification"), any());
    }

    @Test
    void getById() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withActive(true)
                .withDeleted(false);
        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));

        final Optional<AccountBO> retrieved = accountService.getById("");
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(accountBO.withPermissions(expectedPermissions));
    }

    @Test
    void grantPermissions() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(permissionsService.validate(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));

        final List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        final AccountBO updated = accountService.grantPermissions(account.getId(), permissions);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getPermissions()).contains(permissions.toArray(new PermissionBO[0]));
    }

    @Test
    void grantPermissionsInvalidPermission() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        final List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> accountService.grantPermissions(account.getId(), permissions))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void revokePermissions() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);
        final List<PermissionBO> currentPermissions = account.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getGroup())
                        .name(permissionDO.getName())
                        .build()
                ).collect(Collectors.toList());

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        final List<PermissionBO> permissionsToRevoke = Arrays.asList(
                currentPermissions.get(0),
                currentPermissions.get(1)
        );

        final AccountBO updated = accountService.revokePermissions(account.getId(), permissionsToRevoke);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getPermissions()).doesNotContain(permissionsToRevoke.toArray(new PermissionBO[0]));
    }

    @Test
    void grantRoles() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        Mockito.when(rolesService.verifyRoles(roles)).thenReturn(roles);

        final AccountBO updated = accountService.grantRoles(account.getId(), roles);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getRoles()).contains(roles.toArray(new String[0]));
    }

    @Test
    void grantRolesInvalidRoles() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        final List<String> validRoles = Collections.singletonList(roles.get(0));

        Mockito.when(rolesService.verifyRoles(roles)).thenReturn(validRoles);

        assertThatThrownBy(() -> accountService.grantRoles(account.getId(), roles))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void revokeRoles() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);
        final List<String> currentRoles = new ArrayList<>(account.getRoles());

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> rolesToRevoke = Arrays.asList(
                currentRoles.get(0),
                currentRoles.get(1)
        );

        final AccountBO updated = accountService.revokeRoles(account.getId(), rolesToRevoke);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getRoles()).doesNotContain(rolesToRevoke.toArray(new String[0]));
    }

    @Test
    void updatePrimaryEmail() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class);
        final AccountDO accountDO = serviceMapper.toDO(accountBO);
        final AccountEmailBO email = RANDOM.nextObject(AccountEmailBO.class);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        RANDOM.nextObject(AccountEmailBO.class);

        final Optional<AccountBO> updated = accountService.updateEmail(accountDO.getId(), email, false);
        final AccountBO expected = accountBO.withEmail(email);
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).contains(expected.withPermissions(expectedPermissions));

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus).publish(eq("verification"), any());
    }

    @Test
    void updateBackupEmail() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class);
        final AccountDO accountDO = serviceMapper.toDO(accountBO);
        final AccountEmailBO email = RANDOM.nextObject(AccountEmailBO.class);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        RANDOM.nextObject(AccountEmailBO.class);

        final Optional<AccountBO> updated = accountService.updateEmail(accountDO.getId(), email, true);
        final AccountBO expected = accountBO.withBackupEmail(email);
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).contains(expected.withPermissions(expectedPermissions));

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus).publish(eq("verification"), any());
    }

    @Test
    void activateAccount() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withActive(true)
                .withDeleted(false);
        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final AccountBO updated = accountService.activate(accountDO.getId()).orElse(null);
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isNotNull();
        assertThat(updated)
                .isEqualToIgnoringGivenFields(accountBO.withPermissions(expectedPermissions), "active");
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void deactivateAccount() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withActive(true)
                .withDeleted(false);
        final AccountDO accountDO = serviceMapper.toDO(accountBO);
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final AccountBO updated = accountService.deactivate(accountDO.getId()).orElse(null);

        assertThat(updated).isNotNull();
        assertThat(updated)
                .isEqualToIgnoringGivenFields(accountBO.withPermissions(expectedPermissions), "active");
        assertThat(updated.isActive()).isFalse();
    }
}