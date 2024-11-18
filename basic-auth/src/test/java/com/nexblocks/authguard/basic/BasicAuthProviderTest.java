package com.nexblocks.authguard.basic;


import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;

class BasicAuthProviderTest {
    private AccountsService accountsService;
    private SecurePasswordProvider securePasswordProvider;
    private SecurePassword securePassword;
    private SecurePassword previousSecurePassword;
    private TrackingSessionsService trackingSessionsService;

    private BasicAuthProvider basicAuth;

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        securePassword = Mockito.mock(SecurePassword.class);
        previousSecurePassword = Mockito.mock(SecurePassword.class);

        securePasswordProvider = Mockito.mock(SecurePasswordProvider.class);
        trackingSessionsService = Mockito.mock(TrackingSessionsService.class);

        Mockito.when(securePasswordProvider.get()).thenReturn(securePassword);
        Mockito.when(securePasswordProvider.getPreviousVersions())
                .thenReturn(ImmutableMap.of(0, previousSecurePassword));
        Mockito.when(securePasswordProvider.getCurrentVersion())
                .thenReturn(1);
        Mockito.when(trackingSessionsService.startSession(Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(SessionBO.builder()
                        .id(1)
                        .sessionToken("tracking-token")
                        .build()));

        basicAuth = new BasicAuthProvider(accountsService, securePasswordProvider, trackingSessionsService);
    }

    private AccountBO createCredentials(String username) {
        return AccountBO.builder()
                .id(1)
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
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        AccountBO credentials = createCredentials(username);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        AccountBO account = basicAuth.authenticateAndGetAccount(authorization).join();

        assertThat(account).isEqualTo(credentials);
    }

    @Test
    void authenticateInactiveAccount() {
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        AccountBO credentials = createCredentials(username)
                .withActive(false);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateInactiveIdentifier() {
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        AccountBO credentials = createCredentials(username)
                .withIdentifiers(UserIdentifierBO.builder()
                        .identifier(username)
                        .type(UserIdentifier.Type.USERNAME)
                        .active(false)
                        .build());

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));

        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateNotFound() {
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateWrongPassword() {
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        AccountBO credentials = createCredentials(username);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(false);

        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateExpiredPassword() {
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        AccountBO credentials = createCredentials(username)
                .withPasswordUpdatedAt(Instant.now().minus(Duration.ofMinutes(5)));

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);

        Mockito.when(securePasswordProvider.passwordsExpire()).thenReturn(true);
        Mockito.when(securePasswordProvider.getPasswordTtl()).thenReturn(Duration.ofMinutes(2));

        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateWithPreviousPasswordVersion() {
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        AccountBO credentials = createCredentials(username)
                .withActive(true)
                .withPasswordVersion(0);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(previousSecurePassword.verify(eq(password), eq(credentials.getHashedPassword())))
                .thenReturn(true);

        AccountBO account = basicAuth.authenticateAndGetAccount(authorization).join();

        assertThat(account).isEqualTo(credentials);
    }

    @Test
    void authenticateWithPreviousPasswordVersionWrongPassword() {
        String username = "username";
        String password = "password";
        String authorization = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        AccountBO credentials = createCredentials(username)
                .withActive(true)
                .withPasswordVersion(0);

        Mockito.when(accountsService.getByIdentifierUnsafe(username, "global"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(securePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(true);
        Mockito.when(previousSecurePassword.verify(eq(password), eq(credentials.getHashedPassword()))).thenReturn(false);

        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateBadBasicScheme() {
        String authorization = "dGhpc2RvbmVzbid0Zmx5aW5vdXJjaXR5";
        assertThatThrownBy(() -> basicAuth.authenticateAndGetAccount(authorization)).isInstanceOf(ServiceException.class);
    }
}