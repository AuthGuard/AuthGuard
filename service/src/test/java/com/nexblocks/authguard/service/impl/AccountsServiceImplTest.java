package com.nexblocks.authguard.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.nexblocks.authguard.basic.config.PasswordConditions;
import com.nexblocks.authguard.basic.config.PasswordsConfig;
import com.nexblocks.authguard.basic.passwords.PasswordValidator;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
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
import com.nexblocks.authguard.service.util.CredentialsManager;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AccountsServiceImplTest {
    private AccountsRepository accountsRepository;
    private PermissionsService permissionsService;
    private IdempotencyService idempotencyService;
    private RolesService rolesService;
    private MessageBus messageBus;
    private AccountsServiceImpl accountService;
    private SecurePassword securePassword;
    private ServiceMapper serviceMapper;

    private static final EasyRandom RANDOM = new EasyRandom(
            new EasyRandomParameters().collectionSizeRange(3, 5)
    );

    private void compareAccounts(AccountBO actual, AccountBO expected) {
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("id",
                        "createdAt", "lastModified", "plainPassword", "hashedPassword",
                        "passwordUpdatedAt").isEqualTo(expected);
    }

    @BeforeEach
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        permissionsService = Mockito.mock(PermissionsService.class);
        rolesService = Mockito.mock(RolesService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        securePassword = Mockito.mock(SecurePassword.class);
        messageBus = Mockito.mock(MessageBus.class);

        SecurePasswordProvider securePasswordProvider = Mockito.mock(SecurePasswordProvider.class);

        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        AccountConfig accountConfig = AccountConfig.builder()
                .verifyEmail(true)
                .verifyPhoneNumber(true)
                .defaultRolesByDomain(ImmutableMap.of(
                        "unit", Collections.singleton("def-role"),
                        "main", Collections.singleton("not-def-role")))
                .build();

        Mockito.when(configContext.asConfigBean(AccountConfig.class))
                .thenReturn(accountConfig);

        Mockito.when(securePasswordProvider.get()).thenReturn(securePassword);
        Mockito.when(securePasswordProvider.getCurrentVersion())
                .thenReturn(1);

        PasswordValidator passwordValidator = new PasswordValidator(PasswordsConfig.builder()
                .conditions(PasswordConditions.builder().build()).build());

        CredentialsManager credentialsManager = new CredentialsManager(securePasswordProvider, passwordValidator);

        serviceMapper = new ServiceMapperImpl();
        accountService = new AccountsServiceImpl(accountsRepository, permissionsService, rolesService,
                credentialsManager, idempotencyService, serviceMapper, messageBus, configContext);
    }

    private AccountBO createAccountBO() {
        // TODO don't create random objects
        return RANDOM.nextObject(AccountBO.class)
                .withPlainPassword("valid-password")
                .withActive(true)
                .withDeleted(false)
                .withDomain("main");
    }

    private AccountDO createAccountDO() {
        AccountDO account = RANDOM.nextObject(AccountDO.class);

        account.setDomain("main");

        return account;
    }

    @Test
    void create() {
        AccountBO account = createAccountBO()
                .withPlainPassword("test-password")
                .withId(0);

        String idempotentKey = "idempotent-key";
        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountDO.class)));

        Mockito.when(idempotencyService.performOperationAsync(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(account.getEntityType())))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(securePassword.hash(any())).thenReturn(HashedPasswordBO.builder()
                .password("hashed")
                .salt("salted")
                .build());

        Mockito.when(rolesService.verifyRoles(account.getRoles(), "main", EntityType.ACCOUNT))
                .thenReturn(new ArrayList<>(account.getRoles()));

        Mockito.when(permissionsService.validate(account.getPermissions(), "main", EntityType.ACCOUNT))
                .thenReturn(new ArrayList<>(account.getPermissions()));

        AccountBO persisted = accountService.create(account, requestContext).join();

        List<PermissionBO> expectedPermissions = account.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        AccountBO expectedAccount = AccountBO.builder()
                .from(account)
                .identifiers(
                        Streams.concat(
                                account.getIdentifiers().stream(),
                                Stream.of(
                                        UserIdentifierBO.builder()
                                                .domain(account.getDomain())
                                                .active(true)
                                                .identifier(account.getEmail().getEmail())
                                                .type(UserIdentifier.Type.EMAIL)
                                                .build(),
                                        UserIdentifierBO.builder()
                                                .domain(account.getDomain())
                                                .active(true)
                                                .identifier(account.getPhoneNumber().getNumber())
                                                .type(UserIdentifier.Type.PHONE_NUMBER)
                                                .build()
                                )
                        ).collect(Collectors.toList())
                )
                .permissions(expectedPermissions)
                .passwordVersion(1)
                .build();

        assertThat(persisted).isNotNull();
        compareAccounts(persisted, expectedAccount);
        assertThat(persisted.getHashedPassword()).isNull();
        assertThat(persisted.getPlainPassword()).isNull();

        // need better assertion
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

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
    void createWithoutRoles() {
        AccountBO account = createAccountBO()
                .withDomain("unit")
                .withRoles(Collections.emptySet())
                .withPermissions(Collections.emptySet())
                .withId(0);

        List<String> defaultRoles = Collections.singletonList("def-role");

        String idempotentKey = "idempotent-key";
        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountDO.class)));

        Mockito.when(idempotencyService.performOperationAsync(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(account.getEntityType())))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        Mockito.when(rolesService.verifyRoles(new HashSet<>(defaultRoles), "unit", EntityType.ACCOUNT))
                .thenReturn(defaultRoles);

        AccountBO expectedAccount = AccountBO.builder()
                .from(account)
                .identifiers(
                        Streams.concat(
                                account.getIdentifiers().stream(),
                                Stream.of(
                                        UserIdentifierBO.builder()
                                                .domain(account.getDomain())
                                                .active(true)
                                                .identifier(account.getEmail().getEmail())
                                                .type(UserIdentifier.Type.EMAIL)
                                                .build(),
                                        UserIdentifierBO.builder()
                                                .domain(account.getDomain())
                                                .active(true)
                                                .identifier(account.getPhoneNumber().getNumber())
                                                .type(UserIdentifier.Type.PHONE_NUMBER)
                                                .build()
                                )
                        ).collect(Collectors.toList())
                )
                .roles(Collections.singleton("def-role"))
                .passwordVersion(1)
                .build();

        AccountBO persisted = accountService.create(account, requestContext).join();

        assertThat(persisted).isNotNull();
        compareAccounts(persisted, expectedAccount);

        // need better assertion
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

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
        AccountBO accountBO = createAccountBO();
        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));

        Optional<AccountBO> retrieved = accountService.getById(0, "main").join();
        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(retrieved).isPresent();
        compareAccounts(retrieved.get(), accountBO.withPermissions(expectedPermissions));
    }

    @Test
    void getByEmail() {
        AccountBO accountBO = createAccountBO();
        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getByEmail(accountBO.getEmail().getEmail(), accountBO.getDomain()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));

        Optional<AccountBO> retrieved = accountService.getByEmail(accountBO.getEmail().getEmail(), accountBO.getDomain()).join();
        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(retrieved).isPresent();
        compareAccounts(retrieved.get(), accountBO.withPermissions(expectedPermissions));
    }

    @Test
    void grantPermissions() {
        AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(permissionsService.validate(any(), eq("main"), eq(EntityType.ACCOUNT)))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class).withEntityType(null),
                RANDOM.nextObject(PermissionBO.class).withEntityType(null)
        );

        Optional<AccountBO> updated = accountService.grantPermissions(account.getId(), permissions, "main").join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(account);
        assertThat(updated.get().getPermissions()).contains(permissions.toArray(new PermissionBO[0]));
    }

    @Test
    void grantPermissionsInvalidPermission() {
        AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> accountService.grantPermissions(account.getId(), permissions, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void grantPermissionsFromDifferentDomain() {
        AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(permissionsService.validate(any(), eq("other"), eq(EntityType.ACCOUNT)))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));

        List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> accountService.grantPermissions(account.getId(), permissions, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void revokePermissions() {
        AccountDO account = createAccountDO();
        List<PermissionBO> currentPermissions = account.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getGroup())
                        .name(permissionDO.getName())
                        .build()
                ).collect(Collectors.toList());

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        List<PermissionBO> permissionsToRevoke = Arrays.asList(
                currentPermissions.get(0),
                currentPermissions.get(1)
        );

        Optional<AccountBO> updated = accountService.revokePermissions(account.getId(), permissionsToRevoke, "main").join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(account);
        assertThat(updated.get().getPermissions()).doesNotContain(permissionsToRevoke.toArray(new PermissionBO[0]));
    }

    @Test
    void grantRoles() {
        AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        Mockito.when(rolesService.verifyRoles(roles, "main", EntityType.ACCOUNT)).thenReturn(roles);

        Optional<AccountBO> updated = accountService.grantRoles(account.getId(), roles, "main").join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(account);
        assertThat(updated.get().getRoles()).contains(roles.toArray(new String[0]));
    }

    @Test
    void grantRolesInvalidRoles() {
        AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        List<String> validRoles = Collections.singletonList(roles.get(0));

        Mockito.when(rolesService.verifyRoles(roles, "main", EntityType.ACCOUNT)).thenReturn(validRoles);

        assertThatThrownBy(() -> accountService.grantRoles(account.getId(), roles, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void grantRolesFromDifferentDomain() {
        AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        Mockito.when(rolesService.verifyRoles(roles, "other", EntityType.ACCOUNT)).thenReturn(roles);

        assertThatThrownBy(() -> accountService.grantRoles(account.getId(), roles, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void revokeRoles() {
        AccountDO account = createAccountDO();
        List<String> currentRoles = new ArrayList<>(account.getRoles());

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        List<String> rolesToRevoke = Arrays.asList(
                currentRoles.get(0),
                currentRoles.get(1)
        );

        Optional<AccountBO> updated = accountService.revokeRoles(account.getId(), rolesToRevoke, "main").join();

        assertThat(updated).isPresent();
        assertThat(serviceMapper.toDO(updated.get())).isNotEqualTo(account);
        assertThat(updated.get().getRoles()).doesNotContain(rolesToRevoke.toArray(new String[0]));
    }

    @Test
    void patchNoIdentifiers() {
        AccountBO accountBO = createAccountBO()
                .withIdentifiers(Collections.emptyList())
                .withCreatedAt(Instant.now())
                .withLastModified(Instant.now());

        AccountBO update = AccountBO.builder()
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

        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update, "main").join();
        AccountBO expected = accountBO
                .withIdentifiers(Arrays.asList(
                        UserIdentifierBO.builder()
                                .identifier(update.getEmail().getEmail())
                                .type(UserIdentifier.Type.EMAIL)
                                .domain("main")
                                .active(true)
                                .build(),
                        UserIdentifierBO.builder()
                                .identifier(update.getPhoneNumber().getNumber())
                                .type(UserIdentifier.Type.PHONE_NUMBER)
                                .domain("main")
                                .active(true)
                                .build()
                ))
                .withFirstName(update.getFirstName())
                .withMiddleName(update.getMiddleName())
                .withLastName(update.getLastName())
                .withPhoneNumber(update.getPhoneNumber())
                .withEmail(update.getEmail())
                .withBackupEmail(update.getBackupEmail());
        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isPresent();
        compareAccounts(updated.get(), expected.withPermissions(expectedPermissions));
        assertThat(updated.get().getLastModified()).isAfter(expected.getLastModified());

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus, Mockito.times(3))
                .publish(eq("verification"), any());
    }

    @Test
    void patchReplaceIdentifiers() {
        AccountBO accountBO = createAccountBO()
                .withCreatedAt(Instant.now())
                .withLastModified(Instant.now());

        accountBO = accountBO.withIdentifiers(UserIdentifierBO.builder()
                        .type(UserIdentifier.Type.EMAIL)
                        .domain(accountBO.getDomain())
                        .identifier(accountBO.getEmail().getEmail())
                        .build());

        AccountBO update = AccountBO.builder()
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

        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update, "main").join();
        AccountBO expected = accountBO
                .withIdentifiers(Arrays.asList(
                        UserIdentifierBO.builder()
                                .identifier(update.getEmail().getEmail())
                                .type(UserIdentifier.Type.EMAIL)
                                .domain("main")
                                .active(true)
                                .build(),
                        UserIdentifierBO.builder()
                                .identifier(update.getPhoneNumber().getNumber())
                                .type(UserIdentifier.Type.PHONE_NUMBER)
                                .domain("main")
                                .active(true)
                                .build()
                ))
                .withFirstName(update.getFirstName())
                .withMiddleName(update.getMiddleName())
                .withLastName(update.getLastName())
                .withPhoneNumber(update.getPhoneNumber())
                .withEmail(update.getEmail())
                .withBackupEmail(update.getBackupEmail());
        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isPresent();
        compareAccounts(updated.get(), expected.withPermissions(expectedPermissions));
        assertThat(updated.get().getLastModified()).isAfter(expected.getLastModified());

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus, Mockito.times(3))
                .publish(eq("verification"), any());
    }

    @Test
    void addEmail() {
        AccountBO accountBO = createAccountBO()
                .withEmail(null)
                .withIdentifiers(Collections.emptyList());

        AccountBO update = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("new_primary")
                        .build())
                .build();

        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update, "main").join();

        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        AccountBO expected = accountBO
                .withIdentifiers(Arrays.asList(
                        UserIdentifierBO.builder()
                                .identifier(update.getEmail().getEmail())
                                .type(UserIdentifier.Type.EMAIL)
                                .domain("main")
                                .active(true)
                                .build()
                ))
                .withEmail(update.getEmail())
                .withPermissions(expectedPermissions);

        assertThat(updated).isPresent();
        compareAccounts(updated.get(), expected);
    }

    @Test
    void addBackupEmail() {
        AccountBO accountBO = createAccountBO()
                .withBackupEmail(null)
                .withIdentifiers(Collections.emptyList());

        AccountBO update = AccountBO.builder()
                .backupEmail(AccountEmailBO.builder()
                        .email("new_backup")
                        .build())
                .build();

        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update, "main").join();

        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        AccountBO expected = accountBO
                .withBackupEmail(update.getBackupEmail())
                .withPermissions(expectedPermissions);

        assertThat(updated).isPresent();
        compareAccounts(updated.get(), expected);
    }

    @Test
    void addPhoneNumber() {
        AccountBO accountBO = createAccountBO()
                .withPhoneNumber(null)
                .withIdentifiers(Collections.emptyList());

        AccountBO update = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("new_number")
                        .build())
                .build();

        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update, "main").join();

        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        AccountBO expected = accountBO
                .withIdentifiers(Arrays.asList(
                        UserIdentifierBO.builder()
                                .identifier(update.getPhoneNumber().getNumber())
                                .type(UserIdentifier.Type.PHONE_NUMBER)
                                .domain("main")
                                .active(true)
                                .build()
                ))
                .withPhoneNumber(update.getPhoneNumber())
                .withPermissions(expectedPermissions);

        assertThat(updated).isPresent();
        compareAccounts(updated.get(), expected);
    }

    @Test
    void patchWithIdentifiers() {
        AccountBO accountBO = createAccountBO()
                .withCreatedAt(Instant.now())
                .withLastModified(Instant.now());

        accountBO = accountBO.withIdentifiers(Arrays.asList(
                UserIdentifierBO.builder()
                        .identifier("username")
                        .type(UserIdentifier.Type.USERNAME)
                        .domain("main")
                        .active(true)
                        .build(),
                UserIdentifierBO.builder()
                        .identifier(accountBO.getEmail().getEmail())
                        .type(UserIdentifier.Type.EMAIL)
                        .domain("main")
                        .active(true)
                        .build(),
                UserIdentifierBO.builder()
                        .identifier(accountBO.getPhoneNumber().getNumber())
                        .type(UserIdentifier.Type.PHONE_NUMBER)
                        .domain("main")
                        .active(true)
                        .build()
        ));

        AccountBO update = AccountBO.builder()
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

        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update, "main").join();
        AccountBO expected = accountBO
                .withIdentifiers(Arrays.asList(
                        UserIdentifierBO.builder()
                                .identifier("username")
                                .type(UserIdentifier.Type.USERNAME)
                                .domain("main")
                                .active(true)
                                .build(),
                        UserIdentifierBO.builder()
                                .identifier(update.getEmail().getEmail())
                                .type(UserIdentifier.Type.EMAIL)
                                .domain("main")
                                .active(true)
                                .build(),
                        UserIdentifierBO.builder()
                                .identifier(update.getPhoneNumber().getNumber())
                                .type(UserIdentifier.Type.PHONE_NUMBER)
                                .domain("main")
                                .active(true)
                                .build()
                ))
                .withFirstName(update.getFirstName())
                .withMiddleName(update.getMiddleName())
                .withLastName(update.getLastName())
                .withPhoneNumber(update.getPhoneNumber())
                .withEmail(update.getEmail())
                .withBackupEmail(update.getBackupEmail());

        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isPresent();
        compareAccounts(updated.get(), expected.withPermissions(expectedPermissions));
        assertThat(updated.get().getLastModified()).isAfter(expected.getLastModified());

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());

        Mockito.verify(messageBus, Mockito.times(3))
                .publish(eq("verification"), any());
    }

    @Test
    void activateAccount() {
        AccountBO accountBO = createAccountBO();
        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        AccountBO updated = accountService.activate(accountDO.getId(), "main").join().orElse(null);
        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(updated).isNotNull();
        compareAccounts(updated, accountBO.withPermissions(expectedPermissions));
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void deactivateAccount() {
        AccountBO accountBO = createAccountBO();
        AccountDO accountDO = serviceMapper.toDO(accountBO);
        List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        AccountBO updated = accountService.deactivate(accountDO.getId(), "main").join().orElse(null);

        assertThat(updated).isNotNull();
        compareAccounts(updated, accountBO.withPermissions(expectedPermissions).withActive(false));
        assertThat(updated.isActive()).isFalse();
    }
}