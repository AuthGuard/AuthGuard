package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.AuthenticationService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.config.AuthenticationConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountLockBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationServiceImplTest {
    private ExchangeService exchangeService;
    private AccountLocksService accountLocksService;
    private AuthenticationService authenticationService;

    private static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        exchangeService = Mockito.mock(ExchangeService.class);
        accountLocksService = Mockito.mock(AccountLocksService.class);

        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        AuthenticationConfig config = AuthenticationConfig.builder()
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
        String username = "username";
        String password = "password";
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier(username)
                .password(password)
                .build();

        AuthResponseBO tokens = RANDOM.nextObject(AuthResponseBO.class);
        RequestContextBO requestContext = RequestContextBO.builder().build();

        Mockito.when(exchangeService.exchange(authRequest, "basic", "accessToken", requestContext))
                .thenReturn(CompletableFuture.completedFuture(tokens));

        Mockito.when(accountLocksService.getActiveLocksByAccountId(tokens.getEntityId()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        AuthResponseBO result = authenticationService.authenticate(authRequest, requestContext).join();

        assertThat(result).isEqualTo(tokens);
    }

    @Test
    void authenticateLockedAccount() {
        String username = "username";
        String password = "password";
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier(username)
                .password(password)
                .build();

        AuthResponseBO tokens = RANDOM.nextObject(AuthResponseBO.class);
        RequestContextBO requestContext = RequestContextBO.builder().build();

        Mockito.when(exchangeService.exchange(authRequest, "basic", "accessToken", requestContext))
                .thenReturn(CompletableFuture.completedFuture(tokens));

        Mockito.when(accountLocksService.getActiveLocksByAccountId(tokens.getEntityId()))
                .thenReturn(CompletableFuture.completedFuture(Collections.singleton(AccountLockBO.builder().build())));

        assertThatThrownBy(() -> authenticationService.authenticate(authRequest, requestContext).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class)
                .cause()
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_IS_LOCKED.getCode());
    }

    @Test
    void refresh() {
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token("refresh_token")
                .build();

        AuthResponseBO tokens = RANDOM.nextObject(AuthResponseBO.class);
        RequestContextBO requestContext = RequestContextBO.builder().build();

        Mockito.when(exchangeService.exchange(authRequest, "refresh", "accessToken", requestContext))
                .thenReturn(CompletableFuture.completedFuture(tokens));

        AuthResponseBO result = authenticationService.refresh(authRequest, requestContext).join();

        assertThat(result).isEqualTo(tokens);
    }
}
