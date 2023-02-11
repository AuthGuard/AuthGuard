package com.nexblocks.authguard.jwt.exchange.apikeys;

import com.nexblocks.authguard.jwt.ApiTokenVerifier;
import com.nexblocks.authguard.jwt.JwtApiKeyProvider;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtApiKeyExchangeTest {
    private JwtApiKeyProvider apiKeyProvider;
    private ApiTokenVerifier apiTokenVerifier;

    private JwtApiKeyExchange exchange;

    @BeforeEach
    void setup() {
        apiKeyProvider = Mockito.mock(JwtApiKeyProvider.class);
        apiTokenVerifier = Mockito.mock(ApiTokenVerifier.class);

        exchange = new JwtApiKeyExchange(apiKeyProvider, apiTokenVerifier);
    }

    @Test
    void generateKeyWithoutExpiry() {
        final AppBO app = AppBO.builder()
                .id("appId")
                .build();

        final AuthResponseBO expected = AuthResponseBO.builder()
                .id("authResponse")
                .build();

        Mockito.when(apiKeyProvider.generateToken(app))
                        .thenReturn(expected);

        final AuthResponseBO actual = exchange.generateKey(app, null);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void generateKeyWithExpiry() {
        final AppBO app = AppBO.builder()
                .id("appId")
                .build();
        final Instant expiresAt = Instant.now().plusSeconds(10);

        final AuthResponseBO expected = AuthResponseBO.builder()
                .id("authResponse")
                .build();

        Mockito.when(apiKeyProvider.generateToken(app, expiresAt))
                .thenReturn(expected);

        final AuthResponseBO actual = exchange.generateKey(app, expiresAt);

        assertThat(actual).isEqualTo(expected);
    }
}