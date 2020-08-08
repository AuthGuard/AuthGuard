package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.emb.MessageBus;
import com.authguard.service.*;
import com.authguard.service.config.AuthenticationConfig;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationServiceImplTest {
    private ExchangeService exchangeService;
    private MessageBus messageBus;
    private AuthenticationService authenticationService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        exchangeService = Mockito.mock(ExchangeService.class);
        messageBus = Mockito.mock(MessageBus.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        final AuthenticationConfig config = AuthenticationConfig.builder()
                .generateToken("accessToken")
                .build();

        Mockito.when(exchangeService.supportsExchange("basic", "accessToken")).thenReturn(true);
        Mockito.when(configContext.asConfigBean(AuthenticationConfig.class)).thenReturn(config);

        authenticationService = new AuthenticationServiceImpl(exchangeService, messageBus, configContext);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(exchangeService);
    }

    @Test
    void authenticate() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        final TokensBO tokens = RANDOM.nextObject(TokensBO.class);

        Mockito.when(exchangeService.exchange(authorization, "basic", "accessToken"))
                .thenReturn(tokens);
        final Optional<TokensBO> result = authenticationService.authenticate(authorization);

        assertThat(result).isPresent().contains(tokens);

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("auth"), any());
    }
}
