package com.nexblocks.authguard.service.impl;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.config.PasswordConditions;
import com.nexblocks.authguard.basic.config.PasswordsConfig;
import com.nexblocks.authguard.basic.passwords.PasswordValidator;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.basic.passwords.ServiceInvalidPasswordException;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountDO;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.model.CredentialsAuditDO;
import com.nexblocks.authguard.dal.persistence.CredentialsAuditRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.HashedPasswordBO;
import com.nexblocks.authguard.service.model.PasswordResetTokenBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import com.nexblocks.authguard.service.util.CredentialsManager;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
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
    private AccountCredentialsServiceImpl accountCredentialsService;
    private MessageBus messageBus;
    private ServiceMapper serviceMapper;

    private static final String[] SKIPPED_FIELDS = new String[] {
            "lastModified", "hashedPassword", "plainPassword", "passwordUpdatedAt"
    };

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        accountAuditRepository = Mockito.mock(CredentialsAuditRepository.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        messageBus = Mockito.mock(MessageBus.class);

        SecurePasswordProvider securePasswordProvider = Mockito.mock(SecurePasswordProvider.class);

        serviceMapper = new ServiceMapperImpl();

        Mockito.when(securePasswordProvider.get()).thenReturn(securePassword);
        Mockito.when(securePasswordProvider.getCurrentVersion())
                .thenReturn(1);

        PasswordValidator passwordValidator = new PasswordValidator(PasswordsConfig.builder()
                .conditions(PasswordConditions.builder().build()).build());
        
        CredentialsManager accountManager = new CredentialsManager(securePasswordProvider, passwordValidator);

        accountCredentialsService = new AccountCredentialsServiceImpl(accountsService,
                accountAuditRepository, accountTokensRepository,
                securePasswordProvider, accountManager, messageBus, serviceMapper);
    }

    @Test
    void updatePassword() {
        long accountId = 1;
        String newPassword = "new_password";

        AccountBO accountBO = AccountBO.builder()
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
        
        AccountDO accountDO = serviceMapper.toDO(accountBO);

        Mockito.when(accountsService.getByIdUnsafe(accountId, "main"))
                .thenReturn(CompletableFuture.completedFuture(accountBO));

        Mockito.when(accountsService.update(Mockito.any(), Mockito.eq("main")))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountBO.class))));

        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CredentialsAuditDO.class)));

        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        AccountBO result = accountCredentialsService.updatePassword(accountId, newPassword, "main").join();

        assertThat(result).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(accountBO);
        assertThat(result.getHashedPassword()).isNull();
        assertThat(result.getPlainPassword()).isNull();

        // verify call to audit repository
        ArgumentCaptor<CredentialsAuditDO> argumentCaptor = ArgumentCaptor.forClass(CredentialsAuditDO.class);
        Mockito.verify(accountAuditRepository, Mockito.times(2)).save(argumentCaptor.capture());

        List<CredentialsAuditDO> auditArgs = argumentCaptor.getAllValues();

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
        String identifier = "identifier";
        long accountId = 1;

        AccountBO account = AccountBO.builder()
                .id(accountId)
                .identifiers(new HashSet<>())
                .build();

        // mocks
        Mockito.when(accountsService.getByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        // action
        PasswordResetTokenBO resetToken = accountCredentialsService.generateResetToken(identifier, true, "main").join();

        // verify
        ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        AccountTokenDO persistedToken = accountTokenCaptor.getValue();

        assertThat(resetToken.getToken()).isEqualTo(persistedToken.getToken());
        assertThat(persistedToken.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(31)));
    }

    @Test
    void generateResetTokenNoReturn() {
        // data
        String identifier = "identifier";
        long accountId = 1;

        AccountBO account = AccountBO.builder()
                .id(accountId)
                .identifiers(new HashSet<>())
                .build();

        // mocks
        Mockito.when(accountsService.getByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        // action
        PasswordResetTokenBO resetToken = accountCredentialsService.generateResetToken(identifier, false, "main").join();

        // verify
        ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        AccountTokenDO persistedToken = accountTokenCaptor.getValue();

        assertThat(resetToken.getToken()).isNull();
        assertThat(persistedToken.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(31)));
    }

    @Test
    void generateResetTokenNoCredentials() {
        String identifier = "identifier";

        Mockito.when(accountsService.getByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> accountCredentialsService.generateResetToken(identifier, true, "main").join())
                .hasCauseInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void generateResetTokenNoAccount() {
        String identifier = "identifier";

        Mockito.when(accountsService.getByIdentifier(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> accountCredentialsService.generateResetToken(identifier, true, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void resetPasswordByToken() {
        // data
        String resetToken = "token";
        long accountId = 1;

        AccountTokenDO persistedToken = AccountTokenDO.builder()
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(4)))
                .build();

        String newPassword = "new_password";

        AccountBO accountBO = AccountBO.builder()
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

        Mockito.when(accountsService.getByIdUnsafe(accountId, "main"))
                .thenReturn(CompletableFuture.completedFuture(accountBO));

        Mockito.when(accountsService.update(Mockito.any(), Mockito.eq("main")))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountBO.class))));

        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CredentialsAuditDO.class)));

        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        // action
        AccountBO result = accountCredentialsService.resetPasswordByToken(resetToken, newPassword, "main").join();

        // verify
        assertThat(result).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(accountBO);
        assertThat(result.getHashedPassword()).isNull();
        assertThat(result.getPlainPassword()).isNull();
    }

    @Test
    void resetPasswordWrongToken() {
        String resetToken = "token";
        String newPassword = "new_password";

        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> accountCredentialsService.resetPasswordByToken(resetToken, newPassword, "main").join())
                .hasCauseInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void resetPasswordExpiredToken() {
        String resetToken = "token";
        long accountId = 1;

        AccountTokenDO persistedToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().minus(Duration.ofMinutes(31)))
                .additionalInformation(ImmutableMap.of("accountId", "" + accountId))
                .build();

        String newPassword = "new_password";

        Mockito.when(accountTokensRepository.getByToken(resetToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(persistedToken)));

        assertThatThrownBy(() -> accountCredentialsService.resetPasswordByToken(resetToken, newPassword, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void replacePassword() {
        // data
        String identifier = "username";
        String oldPassword = "old_password";
        String newPassword = "new_password";

        AccountBO accountBO = AccountBO.builder()
                .id(1)
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
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountBO)));

        Mockito.when(accountsService.update(Mockito.any(), Mockito.eq("main")))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountBO.class))));

        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CredentialsAuditDO.class)));

        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());

        Mockito.when(securePassword.verify(oldPassword, accountBO.getHashedPassword()))
                .thenReturn(true);

        // action
        AccountBO result =
                accountCredentialsService.replacePassword(identifier, oldPassword, newPassword, "main").join();

        // verify
        assertThat(result).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(accountBO);
        assertThat(result.getHashedPassword()).isNull();
        assertThat(result.getPlainPassword()).isNull();
        assertThat(result.getPasswordUpdatedAt())
                .isAfter(Instant.now().minusSeconds(2))
                .isBefore(Instant.now());
    }

    @Test
    void replacePasswordWrongPassword() {
        // data
        String identifier = "username";
        String oldPassword = "old_password";
        String newPassword = "new_password";

        AccountBO accountBO = AccountBO.builder()
                .id(1)
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(identifier)
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .salt("salty")
                        .password("hashed")
                        .build())
                .build();
        AccountDO accountDO = serviceMapper.toDO(accountBO);

        // mocks
        Mockito.when(accountsService.getByIdentifierUnsafe(identifier, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountBO)));
        Mockito.when(accountAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));
        Mockito.when(securePassword.hash(newPassword))
                .thenReturn(HashedPasswordBO.builder()
                        .password("hashed_new_password")
                        .build());
        Mockito.when(securePassword.verify(oldPassword, accountBO.getHashedPassword()))
                .thenReturn(false);

        // action
        assertThatThrownBy(() -> accountCredentialsService.replacePassword(identifier, oldPassword, newPassword, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void replacePasswordInvalidPassword() {
        // data
        String identifier = "username";
        String oldPassword = "old_password";
        String newPassword = "bad";

        AccountBO accountBO = AccountBO.builder()
                .id(1)
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
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountBO)));
        Mockito.when(accountsService.update(any(), Mockito.eq("main")))
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
        assertThatThrownBy(() -> accountCredentialsService.replacePassword(identifier, oldPassword, newPassword, "main").join())
                .hasCauseInstanceOf(ServiceInvalidPasswordException.class);
    }

    @Test
    void replaceIdentifier() {
        long accountId = 1;

        AccountBO accountBO = AccountBO.builder()
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

        Mockito.when(accountsService.getByIdUnsafe(accountId, "main"))
                .thenReturn(CompletableFuture.completedFuture(accountBO));
        Mockito.when(accountsService.update(Mockito.any(), Mockito.eq("main")))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountBO.class))));

        UserIdentifierBO newIdentifier = UserIdentifierBO.builder()
                .identifier("new_username")
                .active(true)
                .build();

        AccountBO actual = accountCredentialsService.replaceIdentifier(accountId, "username", newIdentifier, "main").join();

        AccountBO expected = AccountBO.builder()
                .id(accountId)
                .addIdentifiers(newIdentifier)
                .passwordVersion(1)
                .build();

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(expected);
        assertThat(actual.getHashedPassword()).isNull();
        assertThat(actual.getPlainPassword()).isNull();
    }

    @Test
    void replaceIdentifierNoCredentials() {
        long accountId = 1;

        Mockito.when(accountsService.getByIdUnsafe(accountId, "main"))
                .thenReturn(CompletableFuture.failedFuture(new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "")));

        assertThatThrownBy(() -> accountCredentialsService.replaceIdentifier(accountId, "username", null, "main").join())
                .hasCauseInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void replaceIdentifierNoIdentifier() {
        long accountId = 1;

        AccountBO accountBO = AccountBO.builder()
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

        Mockito.when(accountsService.getByIdUnsafe(accountId, "main"))
                .thenReturn(CompletableFuture.completedFuture(accountBO));
        Mockito.when(accountsService.update(any(), Mockito.eq("main")))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountBO.class)));

        assertThatThrownBy(() -> accountCredentialsService.replaceIdentifier(accountId, "none", null, "main").join())
                .hasCauseInstanceOf(ServiceException.class);
    }
}