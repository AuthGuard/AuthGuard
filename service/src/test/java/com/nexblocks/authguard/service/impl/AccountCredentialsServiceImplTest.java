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
import com.nexblocks.authguard.dal.persistence.AccountsRepository;
import com.nexblocks.authguard.dal.persistence.CredentialsAuditRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AccountCredentialsServiceImplTest {
    private AccountsService accountsService;
    private CredentialsAuditRepository accountAuditRepository;
    private AccountTokensRepository accountTokensRepository;
    private SecurePassword securePassword;
    private SecurePasswordProvider securePasswordProvider;
    private AccountCredentialsServiceImpl accountCredentialsService;
    private MessageBus messageBus;
    private ServiceMapper serviceMapper;

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        accountAuditRepository = Mockito.mock(CredentialsAuditRepository.class);
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
        
        final CredentialsManager accountManager = new CredentialsManager(securePasswordProvider, passwordValidator);

        accountCredentialsService = new AccountCredentialsServiceImpl(accountsService,
                accountAuditRepository, accountTokensRepository,
                securePasswordProvider, accountManager, messageBus, serviceMapper);
    }

    private AccountBO createCredentials() {
        return AccountBO.builder()
                .id("account")
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

    private AccountDO createAccountDO() {
        return AccountDO.builder()
                .id("account")
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
    void updatePassword() {
        final String accountId = "account";
        final String newPassword = "new_password";

        final AccountBO accountBO = AccountBO.builder()
                .id(accountId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();
        
        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsService.getById(accountId))
                .thenReturn(Optional.of(accountBO));

        Mockito.when(accountsService.update(Mockito.any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountBO.class)));

        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));

        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        final Optional<AccountBO> result = accountCredentialsService.updatePassword(accountId, newPassword);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualToIgnoringGivenFields(accountBO,
                "lastModified", "hashedPassword", "plainPassword", "passwordUpdatedAt");
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();

        // verify call to audit repository
        final ArgumentCaptor<CredentialsAuditDO> argumentCaptor = ArgumentCaptor.forClass(CredentialsAuditDO.class);
        Mockito.verify(accountAuditRepository, Mockito.times(2)).save(argumentCaptor.capture());

        final List<CredentialsAuditDO> auditArgs = argumentCaptor.getAllValues();

        assertThat(auditArgs.size()).isEqualTo(2);

        assertThat(auditArgs.get(0).getCredentialsId()).isEqualTo(accountDO.getId());
        assertThat(auditArgs.get(0).getAction()).isEqualTo(CredentialsAuditDO.Action.ATTEMPT);
        assertThat(auditArgs.get(0).getPassword()).isNull();

        assertThat(auditArgs.get(1).getCredentialsId()).isEqualTo(accountDO.getId());
        assertThat(auditArgs.get(1).getAction()).isEqualTo(CredentialsAuditDO.Action.UPDATED);
        assertThat(auditArgs.get(1).getPassword()).isNotNull();

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("credentials"), any());
    }

    @Test
    void generateResetToken() {
        // data
        final String identifier = "identifier";
        final String accountId = "account";

        final AccountBO account = AccountBO.builder()
                .id(accountId)
                .identifiers(new HashSet<>())
                .build();

        // mocks
        Mockito.when(accountsService.getByIdentifier(identifier, "main")).thenReturn(Optional.of(account));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountTokenDO.class)));

        // action
        final PasswordResetTokenBO resetToken = accountCredentialsService.generateResetToken(identifier, true, "main");

        // verify
        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        final AccountTokenDO persistedToken = accountTokenCaptor.getValue();

        assertThat(resetToken.getToken()).isEqualTo(persistedToken.getToken());
        assertThat(persistedToken.getExpiresAt())
                .isAfter(OffsetDateTime.now())
                .isBefore(OffsetDateTime.now().plusMinutes(31));
    }

    @Test
    void generateResetTokenNoReturn() {
        // data
        final String identifier = "identifier";
        final String accountId = "account";

        final AccountBO account = AccountBO.builder()
                .id(accountId)
                .identifiers(new HashSet<>())
                .build();

        // mocks
        Mockito.when(accountsService.getByIdentifier(identifier, "main")).thenReturn(Optional.of(account));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountTokenDO.class)));

        // action
        final PasswordResetTokenBO resetToken = accountCredentialsService.generateResetToken(identifier, false, "main");

        // verify
        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        final AccountTokenDO persistedToken = accountTokenCaptor.getValue();

        assertThat(resetToken.getToken()).isNull();
        assertThat(persistedToken.getExpiresAt())
                .isAfter(OffsetDateTime.now())
                .isBefore(OffsetDateTime.now().plusMinutes(31));
    }

    @Test
    void generateResetTokenNoCredentials() {
        final String identifier = "identifier";

        assertThatThrownBy(() -> accountCredentialsService.generateResetToken(identifier, true, "main"))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void generateResetTokenNoAccount() {
        final String identifier = "identifier";
        final String accountId = "account";

        Mockito.when(accountsService.getById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountCredentialsService.generateResetToken(identifier, true, "main"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void resetPasswordByToken() {
        // data
        final String resetToken = "token";
        final String accountId = "account";

        final AccountTokenDO persistedToken = AccountTokenDO.builder()
                .associatedAccountId(accountId)
                .expiresAt(OffsetDateTime.now().plusMinutes(4))
                .build();

        final String newPassword = "new_password";

        final AccountBO accountBO = AccountBO.builder()
                .id(accountId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();

        // mocks
        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(persistedToken)));

        Mockito.when(accountsService.getById(accountId))
                .thenReturn(Optional.of(accountBO));

        Mockito.when(accountsService.update(Mockito.any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountBO.class)));

        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));

        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        // action
        final Optional<AccountBO> result = accountCredentialsService.resetPasswordByToken(resetToken, newPassword);

        // verify
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualToIgnoringGivenFields(accountBO,
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

        assertThatThrownBy(() -> accountCredentialsService.resetPasswordByToken(resetToken, newPassword))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void resetPasswordExpiredToken() {
        final String resetToken = "token";
        final String accountId = "account";

        final AccountTokenDO persistedToken = AccountTokenDO.builder()
                .expiresAt(OffsetDateTime.now().minusMinutes(4))
                .additionalInformation(ImmutableMap.of("accountId", accountId))
                .build();

        final String newPassword = "new_password";

        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(persistedToken)));

        assertThatThrownBy(() -> accountCredentialsService.resetPasswordByToken(resetToken, newPassword))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void replacePassword() {
        // data
        final String identifier = "username";
        final String oldPassword = "old_password";
        final String newPassword = "new_password";

        final AccountBO accountBO = AccountBO.builder()
                .id("account")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(identifier)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();

        // mocks
        Mockito.when(accountsService.getByIdentifierUnsafe(identifier, "main"))
                .thenReturn(Optional.of(accountBO));

        Mockito.when(accountsService.update(Mockito.any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountBO.class)));

        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));

        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        Mockito.when(securePassword.verify(oldPassword, accountBO.getHashedPassword()))
                .thenReturn(true);

        // action
        final Optional<AccountBO> result =
                accountCredentialsService.replacePassword(identifier, oldPassword, newPassword, "main");

        // verify
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualToIgnoringGivenFields(accountBO,
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

        final AccountBO accountBO = AccountBO.builder()
                .id("account")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(identifier)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .build();
        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        // mocks
        Mockito.when(accountsService.getByIdentifierUnsafe(identifier, "main"))
                .thenReturn(Optional.of(accountBO));
        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());
        Mockito.when(securePassword.verify(oldPassword, accountBO.getHashedPassword()))
                .thenReturn(false);

        // action
        assertThatThrownBy(() -> accountCredentialsService.replacePassword(identifier, oldPassword, newPassword, "main"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void replacePasswordInvalidPassword() {
        // data
        final String identifier = "username";
        final String oldPassword = "old_password";
        final String newPassword = "bad";

        final AccountBO accountBO = AccountBO.builder()
                .id("account")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(identifier)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .build();

        // mocks
        Mockito.when(accountsService.getByIdentifierUnsafe(identifier, "main"))
                .thenReturn(Optional.of(accountBO));
        Mockito.when(accountsService.update(any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountBO.class)));
        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());
        Mockito.when(securePassword.verify(oldPassword, accountBO.getHashedPassword()))
                .thenReturn(true);

        // action
        assertThatThrownBy(() -> accountCredentialsService.replacePassword(identifier, oldPassword, newPassword, "main"))
                .isInstanceOf(ServiceInvalidPasswordException.class);
    }

    @Test
    void replaceIdentifier() {
        final String accountId = "account";

        final AccountBO accountBO = AccountBO.builder()
                .id(accountId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();

        Mockito.when(accountsService.getByIdUnsafe(accountId))
                .thenReturn(Optional.of(accountBO));
        Mockito.when(accountsService.update(Mockito.any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountBO.class)));

        final UserIdentifierBO newIdentifier = UserIdentifierBO.builder()
                .identifier("new_username")
                .active(true)
                .build();

        final Optional<AccountBO> result = accountCredentialsService.replaceIdentifier(accountId, "username", newIdentifier);

        final AccountBO expected = AccountBO.builder()
                .id(accountId)
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
        final String accountId = "account";

        assertThatThrownBy(() -> accountCredentialsService.replaceIdentifier(accountId, "username", null))
                .isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void replaceIdentifierNoIdentifier() {
        final String accountId = "account";

        final AccountBO accountBO = AccountBO.builder()
                .id(accountId)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier("username")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .passwordVersion(1)
                .build();

        final AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsService.getByIdUnsafe(accountId))
                .thenReturn(Optional.of(accountBO));
        Mockito.when(accountsService.update(any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountBO.class)));

        assertThatThrownBy(() -> accountCredentialsService.replaceIdentifier(accountId, "none", null))
                .isInstanceOf(ServiceException.class);
    }
}