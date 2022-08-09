package com.nexblocks.authguard.service.impl;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.config.PasswordConditions;
import com.nexblocks.authguard.basic.config.PasswordsConfig;
import com.nexblocks.authguard.basic.passwords.PasswordValidator;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.basic.passwords.ServiceInvalidPasswordException;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.*;
import com.nexblocks.authguard.dal.persistence.CredentialsAuditRepository;
import com.nexblocks.authguard.dal.persistence.CredentialsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.*;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class CredentialsServiceImplTest {
    private AccountsService accountsService;
    private IdempotencyService idempotencyService;
    private CredentialsRepository credentialsRepository;
    private CredentialsAuditRepository credentialsAuditRepository;
    private AccountTokensRepository accountTokensRepository;
    private SecurePassword securePassword;
    private SecurePasswordProvider securePasswordProvider;
    private CredentialsServiceImpl credentialsService;
    private MessageBus messageBus;
    private ServiceMapper serviceMapper;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(2, 4));

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        credentialsRepository = Mockito.mock(CredentialsRepository.class);
        credentialsAuditRepository = Mockito.mock(CredentialsAuditRepository.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        securePasswordProvider = Mockito.mock(SecurePasswordProvider.class);
        messageBus = Mockito.mock(MessageBus.class);

        serviceMapper = new ServiceMapperImpl();

        Mockito.when(securePasswordProvider.get()).thenReturn(securePassword);
        Mockito.when(securePasswordProvider.getCurrentVersion())
                .thenReturn(1);

        final PasswordValidator passwordValidator = new PasswordValidator(PasswordsConfig.builder()
                .conditions(PasswordConditions.builder().build()).build());

        credentialsService = new CredentialsServiceImpl(accountsService, idempotencyService,
                credentialsRepository, credentialsAuditRepository, accountTokensRepository,
                securePasswordProvider, passwordValidator, messageBus, serviceMapper);

        Mockito.when(accountsService.getById(any()))
                .thenReturn(Optional.of(RANDOM.nextObject(AccountBO.class)));
    }

    private CredentialsBO createCredentials() {
        return CredentialsBO.builder()
                .id("credentials")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .type(UserIdentifier.Type.USERNAME)
                        .active(true)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .password("hashed")
                        .salt("super-salt")
                        .build())
                .passwordVersion(1)
                .build();
    }

    private CredentialsDO createCredentialsDO() {
        return CredentialsDO.builder()
                .id("credentials")
                .identifiers(Collections.singleton(UserIdentifierDO.builder()
                        .identifier("username")
                        .type(UserIdentifierDO.Type.USERNAME)
                        .active(true)
                        .build()))
                .hashedPassword(PasswordDO.builder()
                        .password("hashed")
                        .salt("super-salt")
                        .build())
                .passwordVersion(1)
                .build();
    }

    @Test
    void create() {
        final CredentialsBO credentials = createCredentials()
                .withPlainPassword("SecurePassword77$3");
        final HashedPasswordBO hashedPassword = RANDOM.nextObject(HashedPasswordBO.class);

        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(securePassword.hash(any())).thenReturn(hashedPassword);
        Mockito.when(credentialsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsDO.class)));

        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(credentials.getEntityType())))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get()));

        final CredentialsBO persisted = credentialsService.create(credentials, requestContext);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(credentials,
                "id", "plainPassword", "hashedPassword",
                "createdAt", "lastModified", "passwordUpdatedAt");
        assertThat(persisted.getHashedPassword()).isNull();

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("credentials"), any());
    }

    @Test
    void createNonExistingAccount() {
        final CredentialsBO credentials = createCredentials();
        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.empty());
        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(credentials.getEntityType())))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get()));

        assertThatThrownBy(() -> credentialsService.create(credentials, requestContext))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void createWithInvalidPassword() {
        final CredentialsBO credentials = createCredentials()
                .withPlainPassword("bad");
        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(credentials.getEntityType())))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get()));

        assertThatThrownBy(() -> credentialsService.create(credentials, requestContext))
                .isInstanceOf(ServiceInvalidPasswordException.class);
    }

    @Test
    void getById() {
        final CredentialsDO credentialsDO = RANDOM.nextObject(CredentialsDO.class);
        final CredentialsBO credentialsBO = serviceMapper.toBO(credentialsDO);

        Mockito.when(credentialsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));

        final Optional<CredentialsBO> retrieved = credentialsService.getById("");

        assertThat(retrieved).contains(credentialsBO
                .withPlainPassword(null)
                .withHashedPassword(null));
    }

    @Test
    void getByIdNotFound() {
        Mockito.when(credentialsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertThat(credentialsService.getById("")).isEmpty();
    }

    @Test
    void getByUsername() {
        final CredentialsDO credentialsDO = RANDOM.nextObject(CredentialsDO.class);
        final CredentialsBO credentialsBO = serviceMapper.toBO(credentialsDO);

        Mockito.when(credentialsRepository.findByIdentifier(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));

        final Optional<CredentialsBO> retrieved = credentialsService.getByUsername("", "main");

        assertThat(retrieved).contains(credentialsBO
                .withPlainPassword(null)
                .withHashedPassword(null));
    }

    @Test
    void getByUsernameUnsafe() {
        final CredentialsDO credentialsDO = createCredentialsDO();
        final CredentialsBO credentialsBO = serviceMapper.toBO(credentialsDO);

        Mockito.when(credentialsRepository.findByIdentifier(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));

        final Optional<CredentialsBO> retrieved = credentialsService.getByUsernameUnsafe("", "main");

        assertThat(retrieved).contains(credentialsBO);
    }

    @Test
    void getByUsernameNotFound() {
        Mockito.when(credentialsRepository.findByIdentifier(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertThat(credentialsService.getByUsername("", "main")).isEmpty();
    }

    @Test
    void updatePassword() {
        final String credentialsId = "credentials";
        final String newPassword = "new_password";

        final CredentialsBO credentialsBO = CredentialsBO.builder()
                .id(credentialsId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();
        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsBO);

        Mockito.when(credentialsRepository.getById(credentialsId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));
        Mockito.when(credentialsAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        final Optional<CredentialsBO> result = credentialsService.updatePassword(credentialsId, newPassword);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualToIgnoringGivenFields(credentialsBO,
                "lastModified", "hashedPassword", "plainPassword", "passwordUpdatedAt");
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();

        // verify call to audit repository
        final ArgumentCaptor<CredentialsAuditDO> argumentCaptor = ArgumentCaptor.forClass(CredentialsAuditDO.class);
        Mockito.verify(credentialsAuditRepository, Mockito.times(2)).save(argumentCaptor.capture());

        final List<CredentialsAuditDO> auditArgs = argumentCaptor.getAllValues();

        assertThat(auditArgs.size()).isEqualTo(2);

        assertThat(auditArgs.get(0).getCredentialsId()).isEqualTo(credentialsDO.getId());
        assertThat(auditArgs.get(0).getAction()).isEqualTo(CredentialsAuditDO.Action.ATTEMPT);
        assertThat(auditArgs.get(0).getPassword()).isNull();

        assertThat(auditArgs.get(1).getCredentialsId()).isEqualTo(credentialsDO.getId());
        assertThat(auditArgs.get(1).getAction()).isEqualTo(CredentialsAuditDO.Action.UPDATED);
        assertThat(auditArgs.get(1).getPassword()).isNotNull();

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("credentials"), any());
    }

    @Test
    void generateResetToken() {
        // data
        final String identifier = "identifier";
        final String credentialsId = "credentials";
        final String accountId = "account";

        final CredentialsDO credentials = CredentialsDO.builder()
                .id(credentialsId)
                .accountId(accountId)
                .identifiers(new HashSet<>())
                .build();
        final AccountBO account = AccountBO.builder()
                .id(accountId)
                .build();

        // mocks
        Mockito.when(credentialsRepository.findByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(accountsService.getById(accountId)).thenReturn(Optional.of(account));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountTokenDO.class)));

        // action
        final PasswordResetTokenBO resetToken = credentialsService.generateResetToken(identifier, true, "main");

        // verify
        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        final AccountTokenDO persistedToken = accountTokenCaptor.getValue();

        assertThat(resetToken.getToken()).isEqualTo(persistedToken.getToken());
        assertThat(persistedToken.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(31)));
        assertThat(persistedToken.getAdditionalInformation().get("credentialsId"))
                .isEqualTo(credentialsId);
    }

    @Test
    void generateResetTokenNoReturn() {
        // data
        final String identifier = "identifier";
        final String credentialsId = "credentials";
        final String accountId = "account";

        final CredentialsDO credentials = CredentialsDO.builder()
                .id(credentialsId)
                .accountId(accountId)
                .identifiers(new HashSet<>())
                .build();
        final AccountBO account = AccountBO.builder()
                .id(accountId)
                .build();

        // mocks
        Mockito.when(credentialsRepository.findByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(accountsService.getById(accountId)).thenReturn(Optional.of(account));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountTokenDO.class)));

        // action
        final PasswordResetTokenBO resetToken = credentialsService.generateResetToken(identifier, false, "main");

        // verify
        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        final AccountTokenDO persistedToken = accountTokenCaptor.getValue();

        assertThat(resetToken.getToken()).isNull();
        assertThat(persistedToken.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(31)));
        assertThat(persistedToken.getAdditionalInformation().get("credentialsId"))
                .isEqualTo(credentialsId);
    }

    @Test
    void generateResetTokenNoCredentials() {
        final String identifier = "identifier";

        Mockito.when(credentialsRepository.findByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> credentialsService.generateResetToken(identifier, true, "main"))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void generateResetTokenNoAccount() {
        final String identifier = "identifier";
        final String credentialsId = "credentials";
        final String accountId = "account";

        final CredentialsDO credentials = CredentialsDO.builder()
                .id(credentialsId)
                .accountId(accountId)
                .identifiers(new HashSet<>())
                .build();

        Mockito.when(credentialsRepository.findByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(accountsService.getById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialsService.generateResetToken(identifier, true, "main"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void resetPasswordByToken() {
        // data
        final String resetToken = "token";
        final String credentialsId = "credentials";

        final AccountTokenDO persistedToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().plus(Duration.ofMinutes(4)))
                .additionalInformation(ImmutableMap.of("credentialsId", credentialsId))
                .build();

        final String newPassword = "new_password";

        final CredentialsBO credentialsBO = CredentialsBO.builder()
                .id(credentialsId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();
        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsBO);

        // mocks
        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(persistedToken)));

        Mockito.when(credentialsRepository.getById(credentialsId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));
        Mockito.when(credentialsAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        // action
        final Optional<CredentialsBO> result = credentialsService.resetPasswordByToken(resetToken, newPassword);

        // verify
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualToIgnoringGivenFields(credentialsBO,
                "lastModified", "hashedPassword", "plainPassword", "passwordUpdatedAt");
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();
    }

    @Test
    void resetPasswordWrongToken() {
        final String resetToken = "token";
        final String newPassword = "new_password";

        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> credentialsService.resetPasswordByToken(resetToken, newPassword))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void resetPasswordExpiredToken() {
        final String resetToken = "token";
        final String credentialsId = "credentials";

        final AccountTokenDO persistedToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().minus(Duration.ofMinutes(4)))
                .additionalInformation(ImmutableMap.of("credentialsId", credentialsId))
                .build();

        final String newPassword = "new_password";

        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(persistedToken)));

        assertThatThrownBy(() -> credentialsService.resetPasswordByToken(resetToken, newPassword))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void resetPasswordNoCredentials() {
        final String resetToken = "token";

        final AccountTokenDO persistedToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().plus(Duration.ofMinutes(4)))
                .build();

        final String newPassword = "new_password";

        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(persistedToken)));

        assertThatThrownBy(() -> credentialsService.resetPasswordByToken(resetToken, newPassword))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void replacePassword() {
        // data
        final String identifier = "username";
        final String oldPassword = "old_password";
        final String newPassword = "new_password";

        final CredentialsBO credentialsBO = CredentialsBO.builder()
                .id("credentials")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(identifier)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();
        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsBO);

        // mocks
        Mockito.when(credentialsRepository.findByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));
        Mockito.when(credentialsAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());
        Mockito.when(securePassword.verify(oldPassword, credentialsBO.getHashedPassword()))
                .thenReturn(true);

        // action
        final Optional<CredentialsBO> result =
                credentialsService.replacePassword(identifier, oldPassword, newPassword, "main");

        // verify
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualToIgnoringGivenFields(credentialsBO,
                "lastModified", "hashedPassword", "plainPassword", "passwordUpdatedAt");
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();
        assertThat(result.get().getPasswordUpdatedAt())
                .isAfter(Instant.now().minusSeconds(2))
                .isBefore(Instant.now());
    }

    @Test
    void replacePasswordWrongPassword() {
        // data
        final String identifier = "username";
        final String oldPassword = "old_password";
        final String newPassword = "new_password";

        final CredentialsBO credentialsBO = CredentialsBO.builder()
                .id("credentials")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(identifier)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .build();
        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsBO);

        // mocks
        Mockito.when(credentialsRepository.findByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));
        Mockito.when(credentialsAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());
        Mockito.when(securePassword.verify(oldPassword, credentialsBO.getHashedPassword()))
                .thenReturn(false);

        // action
        assertThatThrownBy(() -> credentialsService.replacePassword(identifier, oldPassword, newPassword, "main"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void replacePasswordInvalidPassword() {
        // data
        final String identifier = "username";
        final String oldPassword = "old_password";
        final String newPassword = "bad";

        final CredentialsBO credentialsBO = CredentialsBO.builder()
                .id("credentials")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(identifier)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .build();
        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsBO);

        // mocks
        Mockito.when(credentialsRepository.findByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));
        Mockito.when(credentialsAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());
        Mockito.when(securePassword.verify(oldPassword, credentialsBO.getHashedPassword()))
                .thenReturn(true);

        // action
        assertThatThrownBy(() -> credentialsService.replacePassword(identifier, oldPassword, newPassword, "main"))
                .isInstanceOf(ServiceInvalidPasswordException.class);
    }

    @Test
    void replaceIdentifier() {
        final String credentialsId = "credentials";

        final CredentialsBO credentialsBO = CredentialsBO.builder()
                .id(credentialsId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();

        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsBO);

        final UserIdentifierBO newIdentifier = UserIdentifierBO.builder()
                .identifier("new_username")
                .active(true)
                .build();

        Mockito.when(credentialsRepository.getById(credentialsId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));

        final Optional<CredentialsBO> result = credentialsService.replaceIdentifier(credentialsId, "username", newIdentifier);

        final CredentialsBO expected = CredentialsBO.builder()
                .id(credentialsId)
                .addIdentifiers(newIdentifier)
                .passwordVersion(1)
                .build();

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualToIgnoringGivenFields(expected,
                "lastModified", "hashedPassword", "plainPassword");
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();
    }

    @Test
    void replaceIdentifierNoCredentials() {
        final String credentialsId = "credentials";

        Mockito.when(credentialsRepository.getById(credentialsId))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> credentialsService.replaceIdentifier(credentialsId, "username", null))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void replaceIdentifierNoIdentifier() {
        final String credentialsId = "credentials";

        final CredentialsBO credentialsBO = CredentialsBO.builder()
                .id(credentialsId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();

        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsBO);

        Mockito.when(credentialsRepository.getById(credentialsId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentialsDO)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));

        assertThatThrownBy(() -> credentialsService.replaceIdentifier(credentialsId, "none", null))
                .isInstanceOf(ServiceException.class);
    }
}
