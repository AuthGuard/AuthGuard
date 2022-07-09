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
    private SecurePasswordProvider securePasswordProvider;
    private ServiceMapper serviceMapper;
    private CredentialsManager credentialsManager;

    private final static EasyRandom RANDOM = new EasyRandom(
            new EasyRandomParameters().collectionSizeRange(3, 5)
    );

    private void compareAccounts(final AccountBO actual, final AccountBO expected) {
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "id",
                        "createdAt", "lastModified", "plainPassword", "hashedPassword",
                        "passwordUpdatedAt");
    }

    @BeforeEach
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        permissionsService = Mockito.mock(PermissionsService.class);
        rolesService = Mockito.mock(RolesService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        securePassword = Mockito.mock(SecurePassword.class);
        securePasswordProvider = Mockito.mock(SecurePasswordProvider.class);
        messageBus = Mockito.mock(MessageBus.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        final AccountConfig accountConfig = AccountConfig.builder()
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

        final PasswordValidator passwordValidator = new PasswordValidator(PasswordsConfig.builder()
                .conditions(PasswordConditions.builder().build()).build());

        credentialsManager = new CredentialsManager(securePasswordProvider, passwordValidator);

        serviceMapper = new ServiceMapperImpl();
        accountService = new AccountsServiceImpl(accountsRepository, permissionsService, rolesService,
                credentialsManager, idempotencyService, serviceMapper, messageBus, configContext);
    }

    private AccountBO createAccountBO() {
        // TODO don't create random objects
        return RANDOM.nextObject(AccountBO.class)
                .withActive(true)
                .withDeleted(false)
                .withDomain("main");
    }

    private AccountDO createAccountDO() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        account.setDomain("main");

        return account;
    }

    @Test
    void create() {
        final AccountBO account = createAccountBO()
                .withPlainPassword("test-password")
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

        Mockito.when(securePassword.hash(any())).thenReturn(HashedPasswordBO.builder()
                .password("hashed")
                .salt("salted")
                .build());

        Mockito.when(rolesService.verifyRoles(account.getRoles(), "main"))
                .thenReturn(new ArrayList<>(account.getRoles()));

        final AccountBO persisted = accountService.create(account, requestContext);

        final List<PermissionBO> expectedPermissions = account.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        final AccountBO expectedAccount = AccountBO.builder()
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
    void createWithoutRoles() {
        final AccountBO account = createAccountBO()
                .withDomain("unit")
                .withRoles(Collections.emptySet())
                .withPermissions(Collections.emptySet())
                .withId(null);

        final List<String> defaultRoles = Collections.singletonList("def-role");

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

        Mockito.when(rolesService.verifyRoles(new HashSet<>(defaultRoles), "unit")).thenReturn(defaultRoles);

        final AccountBO expectedAccount = AccountBO.builder()
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

        final AccountBO persisted = accountService.create(account, requestContext);

        assertThat(persisted).isNotNull();
        compareAccounts(persisted, expectedAccount);

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
        final AccountBO accountBO = createAccountBO();
        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));

        final Optional<AccountBO> retrieved = accountService.getById("");
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(retrieved).isPresent();
        compareAccounts(retrieved.get(), accountBO.withPermissions(expectedPermissions));
    }

    @Test
    void getByEmail() {
        final AccountBO accountBO = createAccountBO();
        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getByEmail(accountBO.getEmail().getEmail(), accountBO.getDomain()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));

        final Optional<AccountBO> retrieved = accountService.getByEmail(accountBO.getEmail().getEmail(), accountBO.getDomain());
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(retrieved).isPresent();
        compareAccounts(retrieved.get(), accountBO.withPermissions(expectedPermissions));
    }

    @Test
    void grantPermissions() {
        final AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(permissionsService.validate(any(), eq("main")))
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
        final AccountDO account = createAccountDO();

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
    void grantPermissionsFromDifferentDomain() {
        final AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(permissionsService.validate(any(), eq("other")))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));

        final List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> accountService.grantPermissions(account.getId(), permissions))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void revokePermissions() {
        final AccountDO account = createAccountDO();
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
        final AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        Mockito.when(rolesService.verifyRoles(roles, "main")).thenReturn(roles);

        final AccountBO updated = accountService.grantRoles(account.getId(), roles);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getRoles()).contains(roles.toArray(new String[0]));
    }

    @Test
    void grantRolesInvalidRoles() {
        final AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        final List<String> validRoles = Collections.singletonList(roles.get(0));

        Mockito.when(rolesService.verifyRoles(roles, "main")).thenReturn(validRoles);

        assertThatThrownBy(() -> accountService.grantRoles(account.getId(), roles))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void grantRolesFromDifferentDomain() {
        final AccountDO account = createAccountDO();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        Mockito.when(rolesService.verifyRoles(roles, "other")).thenReturn(roles);

        assertThatThrownBy(() -> accountService.grantRoles(account.getId(), roles))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void revokeRoles() {
        final AccountDO account = createAccountDO();
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
    void patchNoIdentifiers() {
        final AccountBO accountBO = createAccountBO()
                .withIdentifiers(Collections.emptyList())
                .withCreatedAt(Instant.now())
                .withLastModified(Instant.now());

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
        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
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
        final AccountBO accountBO = createAccountBO()
                .withEmail(null)
                .withIdentifiers(Collections.emptyList());

        final AccountBO update = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("new_primary")
                        .build())
                .build();

        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update);

        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        final AccountBO expected = accountBO
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
        final AccountBO accountBO = createAccountBO()
                .withBackupEmail(null)
                .withIdentifiers(Collections.emptyList());

        final AccountBO update = AccountBO.builder()
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

        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        final AccountBO expected = accountBO
                .withBackupEmail(update.getBackupEmail())
                .withPermissions(expectedPermissions);

        assertThat(updated).isPresent();
        compareAccounts(updated.get(), expected);
    }

    @Test
    void addPhoneNumber() {
        final AccountBO accountBO = createAccountBO()
                .withPhoneNumber(null)
                .withIdentifiers(Collections.emptyList());

        final AccountBO update = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("new_number")
                        .build())
                .build();

        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsRepository.getById(accountDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountDO)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final Optional<AccountBO> updated = accountService.patch(accountDO.getId(), update);

        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        final AccountBO expected = accountBO
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

        final List<PermissionBO> expectedPermissions = accountBO.getPermissions().stream()
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
        final AccountBO accountBO = createAccountBO();
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
        compareAccounts(updated, accountBO.withPermissions(expectedPermissions));
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void deactivateAccount() {
        final AccountBO accountBO = createAccountBO();
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
        compareAccounts(updated, accountBO.withPermissions(expectedPermissions).withActive(false));
        assertThat(updated.isActive()).isFalse();
    }
}