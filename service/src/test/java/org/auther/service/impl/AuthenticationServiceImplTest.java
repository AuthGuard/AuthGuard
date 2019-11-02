package org.auther.service.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.auther.dal.model.AccountDO;
import org.auther.service.*;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.CredentialsBO;
import org.auther.service.model.HashedPasswordBO;
import org.auther.service.model.TokensBO;
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
    private CredentialsService credentialsService;
    private SecurePassword securePassword;
    private JwtProvider jwtProvider;
    private AuthenticationService authenticationService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        credentialsService = Mockito.mock(CredentialsService.class);
        securePassword = Mockito.mock(SecurePassword.class);
        jwtProvider = Mockito.mock(JwtProvider.class);
        authenticationService = new AuthenticationServiceImpl(credentialsService, accountsService, securePassword, jwtProvider);
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

        Mockito.when(credentialsService.getByUsername(username)).thenReturn(Optional.of(credentials));
        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(eq(password), eq(hashedPasswordBO))).thenReturn(true);
        Mockito.when(jwtProvider.generateToken(any(), any())).thenReturn(tokens);

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

        final AccountDO account = RANDOM.nextObject(AccountDO.class);
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class).withUsername(username);
        final HashedPasswordBO hashedPasswordBO = HashedPasswordBO.builder()
                .password(credentials.getHashedPassword().getPassword())
                .salt(credentials.getHashedPassword().getSalt())
                .build();

        Mockito.when(credentialsService.getByUsername(username)).thenReturn(Optional.of(credentials));
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
