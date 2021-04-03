package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.model.AccountDO;
import com.nexblocks.authguard.dal.persistence.AccountsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.*;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
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
                .verifyPhoneNumber(true)
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
                .isEqualToIgnoringGivenFields(account.withPermissions(expectedPermissions), "id", "createdAt", "lastModified");

        // need better assertion
        final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus, Mockito.times(2))
                .publish(Mockito.eq("verification"), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues().stream()
                .map(Message::getEventType)
                .collect(Collectors.toList()))
                .containsExactly(EventType.EMAIL_VERIFICATION, EventType.PHONE_NUMBER_VERIFICATION);
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
    void getByEmail() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withActive(true)
                .withDeleted(false);
        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getByEmail(accountBO.getEmail().getEmail()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));

        final Optional<AccountBO> retrieved = accountService.getByEmail(accountBO.getEmail().getEmail());
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
    void patch() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withCreatedAt(OffsetDateTime.now())
                .withLastModified(OffsetDateTime.now());

        final AccountBO update = AccountBO.builder()
                .firstName("first_name")
                .middleName("middle_name")
                .lastName("last_name")
                .phoneNumber(PhoneNumberBO.builder()
                        .number("new_number")
                        .build())
                .email(AccountEmailBO.builder()
                        .email("new_primary")
                        .build())
                .backupEmail(AccountEmailBO.builder()
                        .email("new_backup")
                        .build())
                .build();

        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update);
        final AccountBO expected = accountBO
                .withFirstName(update.getFirstName())
                .withMiddleName(update.getMiddleName())
                .withLastName(update.getLastName())
                .withPhoneNumber(update.getPhoneNumber())
                .withEmail(update.getEmail())
                .withBackupEmail(update.getBackupEmail());
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isPresent();
        assertThat(updated.get()).isEqualToIgnoringGivenFields(expected.withPermissions(expectedPermissions), "lastModified");
        assertThat(updated.get().getLastModified()).isAfter(expected.getLastModified());

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus, Mockito.times(3))
                .publish(eq("verification"), any());
    }

    @Test
    void updatePrimaryEmail() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withCreatedAt(OffsetDateTime.now())
                .withLastModified(OffsetDateTime.now());
        final AccountDO accountDO = serviceMapper.toDO(accountBO);
        final AccountEmailBO email = RANDOM.nextObject(AccountEmailBO.class);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final Optional<AccountBO> updated = accountService.updateEmail(accountDO.getId(), email, false);
        final AccountBO expected = accountBO.withEmail(email);
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isPresent();
        assertThat(updated.get()).isEqualToIgnoringGivenFields(expected.withPermissions(expectedPermissions), "lastModified");
        assertThat(updated.get().getLastModified()).isAfter(expected.getLastModified());

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus).publish(eq("verification"), any());
    }

    @Test
    void updateBackupEmail() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withCreatedAt(OffsetDateTime.now())
                .withLastModified(OffsetDateTime.now());
        final AccountDO accountDO = serviceMapper.toDO(accountBO);
        final AccountEmailBO email = RANDOM.nextObject(AccountEmailBO.class);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final Optional<AccountBO> updated = accountService.updateEmail(accountDO.getId(), email, true);
        final AccountBO expected = accountBO.withBackupEmail(email);
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isPresent();
        assertThat(updated.get()).isEqualToIgnoringGivenFields(expected.withPermissions(expectedPermissions), "lastModified");
        assertThat(updated.get().getLastModified()).isAfter(expected.getLastModified());

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus).publish(eq("verification"), any());
    }

    @Test
    void updatePhoneNumber() {
        final AccountBO accountBO = RANDOM.nextObject(AccountBO.class)
                .withCreatedAt(OffsetDateTime.now())
                .withLastModified(OffsetDateTime.now());
        final AccountDO accountDO = serviceMapper.toDO(accountBO);
        final PhoneNumberBO phoneNumber = RANDOM.nextObject(PhoneNumberBO.class);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final Optional<AccountBO> updated = accountService.updatePhoneNumber(accountDO.getId(), phoneNumber);
        final AccountBO expected = accountBO.withPhoneNumber(phoneNumber);
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isPresent();
        assertThat(updated.get()).isEqualToIgnoringGivenFields(expected.withPermissions(expectedPermissions), "lastModified");
        assertThat(updated.get().getLastModified()).isAfter(expected.getLastModified());

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
                .isEqualToIgnoringGivenFields(accountBO.withPermissions(expectedPermissions), "id", "createdAt", "lastModified", "active");
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
                .isEqualToIgnoringGivenFields(accountBO.withPermissions(expectedPermissions), "id", "createdAt", "lastModified", "active");
        assertThat(updated.isActive()).isFalse();
    }
}