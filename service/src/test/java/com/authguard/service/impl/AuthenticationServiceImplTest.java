package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.AccountLocksService;
import com.authguard.service.AuthenticationService;
import com.authguard.service.ExchangeService;
import com.authguard.service.config.AuthenticationConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.AccountLockBO;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationServiceImplTest {
    private ExchangeService exchangeService;
    private AccountLocksService accountLocksService;
    private AuthenticationService authenticationService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        exchangeService = Mockito.mock(ExchangeService.class);
        accountLocksService = Mockito.mock(AccountLocksService.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        final AuthenticationConfig config = AuthenticationConfig.builder()
                .generateToken("accessToken")
                .build();

        Mockito.when(exchangeService.supportsExchange("basic", "accessToken")).thenReturn(true);
        Mockito.when(configContext.asConfigBean(AuthenticationConfig.class)).thenReturn(config);

        authenticationService = new AuthenticationServiceImpl(exchangeService, accountLocksService, configContext);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(exchangeService);
    }

    @Test
    void authenticate() {
        final String username = "username";
        final String password = "password";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier(username)
                .password(password)
                .build();

        final TokensBO tokens = RANDOM.nextObject(TokensBO.class);

        Mockito.when(exchangeService.exchange(authRequest, "basic", "accessToken"))
                .thenReturn(tokens);

        Mockito.when(accountLocksService.getActiveLocksByAccountId(tokens.getEntityId()))
                .thenReturn(Collections.emptyList());

        final Optional<TokensBO> result = authenticationService.authenticate(authRequest);

        assertThat(result).isPresent().contains(tokens);
    }

    @Test
    void authenticateLockedAccount() {
        final String username = "username";
        final String password = "password";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier(username)
                .password(password)
                .build();

        final TokensBO tokens = RANDOM.nextObject(TokensBO.class);

        Mockito.when(exchangeService.exchange(authRequest, "basic", "accessToken"))
                .thenReturn(tokens);

        Mockito.when(accountLocksService.getActiveLocksByAccountId(tokens.getEntityId()))
                .thenReturn(Collections.singleton(AccountLockBO.builder().build()));

        assertThatThrownBy(() -> authenticationService.authenticate(authRequest))
                .isInstanceOf(ServiceAuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_IS_LOCKED);
    }
}
