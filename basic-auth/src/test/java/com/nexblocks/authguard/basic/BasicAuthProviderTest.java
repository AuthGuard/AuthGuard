package com.nexblocks.authguard.basic;


import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.CredentialsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.*;
import io.vavr.control.Either;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;

class BasicAuthProviderTest {
    private AccountsService accountsService;
    private CredentialsService credentialsService;
    private SecurePasswordProvider securePasswordProvider;
    private SecurePassword securePassword;
    private SecurePassword previousSecurePassword;

    private BasicAuthProvider basicAuth;

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        credentialsService = Mockito.mock(CredentialsService.class);
        securePassword = Mockito.mock(SecurePassword.class);
        previousSecurePassword = Mockito.mock(SecurePassword.class);

        securePasswordProvider = Mockito.mock(SecurePasswordProvider.class);

        Mockito.when(securePasswordProvider.get()).thenReturn(securePassword);
        Mockito.when(securePasswordProvider.getPreviousVersions())
                .thenReturn(ImmutableMap.of(0, previousSecurePassword));
        Mockito.when(securePasswordProvider.getCurrentVersion())
                .thenReturn(1);

        basicAuth = new BasicAuthProvider(credentialsService, accountsService, securePasswordProvider);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(accountsService);
        Mockito.reset(credentialsService);
    }

    private CredentialsBO createCredentials(final String username) {
        return CredentialsBO.builder()
                .id("credentials")
                .addIdentifiers(UserIdentifierBO.builder()
                        .identifier(username)
                        .type(UserIdentifier.Type.USERNAME)
                        .active(true)
                        .domain("main")
                        .build())
                .hashedPassword(HashedPasswordBO.builder()
                        .password("hashed")
                        .salt("super-salt")
                        .build())
                .passwordVersion(1)
                .domain("main")
                .build();
    }

    @Test
    void authenticate() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO account = AccountBO.builder()
                .active(true)
                .build();
        final CredentialsBO credentials = createCredentials(username);

        Mockito.when(credentialsService.getByUsernameUnsafe(username, "global")).thenReturn(Optional.of(credentials));
        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.get()).isEqualTo(account);
    }

    @Test
    void authenticateInactiveAccount() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO account = AccountBO.builder()
                .active(false)
                .build();
        final CredentialsBO credentials = createCredentials(username);

        Mockito.when(credentialsService.getByUsernameUnsafe(username, "main")).thenReturn(Optional.of(credentials));
        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateInactiveIdentifier() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final CredentialsBO credentials = createCredentials(username)
                .withIdentifiers(UserIdentifierBO.builder()
                        .identifier(username)
                        .type(UserIdentifier.Type.USERNAME)
                        .active(false)
                        .build());

        Mockito.when(credentialsService.getByUsernameUnsafe(username, "main")).thenReturn(Optional.of(credentials));

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateNotFound() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        Mockito.when(credentialsService.getByUsername(username, "main")).thenReturn(Optional.empty());

        assertThat(basicAuth.authenticateAndGetAccount(authorization)).isEmpty();
    }

    @Test
    void authenticateWrongPassword() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final CredentialsBO credentials = createCredentials(username);

        Mockito.when(credentialsService.getByUsernameUnsafe(username, "main")).thenReturn(Optional.of(credentials));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(false);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateExpiredPassword() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final CredentialsBO credentials = createCredentials(username)
                .withPasswordUpdatedAt(OffsetDateTime.now().minusMinutes(5));

        Mockito.when(credentialsService.getByUsernameUnsafe(username, "main")).thenReturn(Optional.of(credentials));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        Mockito.when(securePasswordProvider.passwordsExpire()).thenReturn(true);
        Mockito.when(securePasswordProvider.getPasswordTtl()).thenReturn(Duration.ofMinutes(2));

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateWithPreviousPasswordVersion() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO account = AccountBO.builder()
                .active(true)
                .build();
        final CredentialsBO credentials = createCredentials(username)
                .withPasswordVersion(0);

        Mockito.when(credentialsService.getByUsernameUnsafe(username, "global"))
                .thenReturn(Optional.of(credentials));
        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.of(account));
        Mockito.when(previousSecurePassword.verify(eq(password), eq(credentials.getHashedPassword())))
                .thenReturn(true);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.get()).isEqualTo(account);
    }

    @Test
    void authenticateWithPreviousPasswordVersionWrongPassword() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO account = AccountBO.builder()
                .active(true)
                .build();
        final CredentialsBO credentials = createCredentials(username)
                .withPasswordVersion(0);

        Mockito.when(credentialsService.getByUsernameUnsafe(username, credentials.getDomain()))
                .thenReturn(Optional.of(credentials));
        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);
        Mockito.when(previousSecurePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(false);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateBadAuthorization() {
        final String authorization = RandomStringUtils.randomAlphanumeric(20);
        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization)).isInstanceOf(ServiceException.class);
    }

    @Test
    void authenticateBadBasicScheme() {
        final String authorization = "dGhpc2RvbmVzbid0Zmx5aW5vdXJjaXR5";
        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization)).isInstanceOf(ServiceException.class);
    }
}