package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.*;
import com.authguard.service.exceptions.ServiceException;
import org.apache.commons.lang3.RandomStringUtils;
import com.authguard.service.config.ImmutableAuthenticationConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.CredentialsBO;
import com.authguard.service.model.HashedPasswordBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationServiceImplTest {
    private AccountsService accountsService;
    private OtpService otpService;
    private CredentialsService credentialsService;
    private SecurePassword securePassword;
    private AuthProvider authProvider;
    private ConfigContext configContext;
    private AuthenticationService authenticationService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        otpService = Mockito.mock(OtpService.class);
        credentialsService = Mockito.mock(CredentialsService.class);
        securePassword = Mockito.mock(SecurePassword.class);
        authProvider = Mockito.mock(AuthProvider.class);
        configContext = Mockito.mock(ConfigContext.class);

        final ImmutableAuthenticationConfig config = ImmutableAuthenticationConfig.builder()
                .useOtp(false)
                .build();

        Mockito.when(configContext.asConfigBean(ImmutableAuthenticationConfig.class)).thenReturn(config);

        authenticationService = new AuthenticationServiceImpl(credentialsService, otpService, accountsService,
                securePassword, authProvider, configContext);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(accountsService);
        Mockito.reset(credentialsService);
    }

    @Test
    void authenticate() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class).withUsername(username);
        final TokensBO tokens = RANDOM.nextObject(TokensBO.class);
        final HashedPasswordBO hashedPasswordBO = HashedPasswordBO.builder()
                .password(credentials.getHashedPassword().getPassword())
                .salt(credentials.getHashedPassword().getSalt())
                .build();

        Mockito.when(credentialsService.getByUsernameUnsafe(username)).thenReturn(Optional.of(credentials));
        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(eq(password), eq(hashedPasswordBO))).thenReturn(true);
        Mockito.when(authProvider.generateToken(any(AccountBO.class))).thenReturn(tokens);

        final Optional<TokensBO> result = authenticationService.authenticate(authorization);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(tokens);
    }

    @Test
    void authenticateNotFound() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        Mockito.when(credentialsService.getByUsername(username)).thenReturn(Optional.empty());

        assertThat(authenticationService.authenticate(authorization)).isEmpty();
    }

    @Test
    void authenticateWrongPassword() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class).withUsername(username);
        final HashedPasswordBO hashedPasswordBO = HashedPasswordBO.builder()
                .password(credentials.getHashedPassword().getPassword())
                .salt(credentials.getHashedPassword().getSalt())
                .build();

        Mockito.when(credentialsService.getByUsernameUnsafe(username)).thenReturn(Optional.of(credentials));
        Mockito.when(securePassword.verify(eq(password), eq(hashedPasswordBO))).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.authenticate(authorization)).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateBadAuthorization() {
        final String authorization = RandomStringUtils.randomAlphanumeric(20);
        assertThatThrownBy(() -> authenticationService.authenticate(authorization)).isInstanceOf(ServiceException.class);
    }

    @Test
    void authenticateUnsupportedScheme() {
        final String authorization = "Unsupported " + RandomStringUtils.randomAlphanumeric(20);
        assertThatThrownBy(() -> authenticationService.authenticate(authorization)).isInstanceOf(ServiceException.class);
    }

    @Test
    void authenticateBadBasicScheme() {
        final String authorization = "Basic dGhpc2RvbmVzbid0Zmx5aW5vdXJjaXR5";
        assertThatThrownBy(() -> authenticationService.authenticate(authorization)).isInstanceOf(ServiceException.class);
    }
}
