package com.nexblocks.authguard.basic;


import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.HashedPasswordBO;
import com.nexblocks.authguard.service.model.UserIdentifier;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import io.vavr.control.Either;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;

class BasicAuthProviderTest {
    private AccountsService accountsService;
    private SecurePasswordProvider securePasswordProvider;
    private SecurePassword securePassword;
    private SecurePassword previousSecurePassword;

    private BasicAuthProvider basicAuth;

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        securePassword = Mockito.mock(SecurePassword.class);
        previousSecurePassword = Mockito.mock(SecurePassword.class);

        securePasswordProvider = Mockito.mock(SecurePasswordProvider.class);

        Mockito.when(securePasswordProvider.get()).thenReturn(securePassword);
        Mockito.when(securePasswordProvider.getPreviousVersions())
                .thenReturn(ImmutableMap.of(0, previousSecurePassword));
        Mockito.when(securePasswordProvider.getCurrentVersion())
                .thenReturn(1);

        basicAuth = new BasicAuthProvider(accountsService, securePasswordProvider);
    }

    private AccountBO createCredentials(final String username) {
        return AccountBO.builder()
                .id("credentials")
                .active(true)
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

        final AccountBO credentials = createCredentials(username);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(Optional.of(credentials));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.get()).isEqualTo(credentials);
    }

    @Test
    void authenticateInactiveAccount() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO credentials = createCredentials(username)
                .withActive(false);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "main")).thenReturn(Optional.of(credentials));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateInactiveIdentifier() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO credentials = createCredentials(username)
                .withIdentifiers(UserIdentifierBO.builder()
                        .identifier(username)
                        .type(UserIdentifier.Type.USERNAME)
                        .active(false)
                        .build());

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "main")).thenReturn(Optional.of(credentials));

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateNotFound() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "main")).thenReturn(Optional.empty());

        assertThat(basicAuth.authenticateAndGetAccount(authorization)).isEmpty();
    }

    @Test
    void authenticateWrongPassword() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO credentials = createCredentials(username);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "main")).thenReturn(Optional.of(credentials));
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

        final AccountBO credentials = createCredentials(username)
                .withPasswordUpdatedAt(Instant.now().minus(Duration.ofMinutes(5)));

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "main")).thenReturn(Optional.of(credentials));
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

        final AccountBO credentials = createCredentials(username)
                .withActive(true)
                .withPasswordVersion(0);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(Optional.of(credentials));
        Mockito.when(previousSecurePassword.verify(eq(password), eq(credentials.getHashedPassword())))
                .thenReturn(true);

        final Either<Exception, AccountBO> result = basicAuth.authenticateAndGetAccount(authorization);

        assertThat(result.get()).isEqualTo(credentials);
    }

    @Test
    void authenticateWithPreviousPasswordVersionWrongPassword() {
        final String username = "username";
        final String password = "password";
        final String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO credentials = createCredentials(username)
                .withActive(true)
                .withPasswordVersion(0);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, credentials.getDomain()))
                .thenReturn(Optional.of(credentials));
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