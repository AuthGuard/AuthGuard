package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.jwt.IdTokenProvider;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class BasicToOIdCTest {
    private BasicAuthProvider basicAuth;
    private AccessTokenProvider accessTokenProvider;
    private IdTokenProvider idTokenProvider;
    private OpenIdConnectTokenProvider openIdConnectTokenProvider;

    private BasicToOIdC basicToOIdC;

    @BeforeEach
    void setup() {
        basicAuth = Mockito.mock(BasicAuthProvider.class);
        accessTokenProvider = Mockito.mock(AccessTokenProvider.class);
        idTokenProvider = Mockito.mock(IdTokenProvider.class);

        openIdConnectTokenProvider = new OpenIdConnectTokenProvider(accessTokenProvider, idTokenProvider);

        basicToOIdC = new BasicToOIdC(basicAuth, openIdConnectTokenProvider);
    }

    @Test
    void exchange() {
        AccountBO account = AccountBO.builder().id(101).build();

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier("username")
                .password("password")
                .build();

        AuthResponseBO accessTokenResponse = AuthResponseBO.builder()
                .token("access token")
                .refreshToken("refresh token")
                .build();

        AuthResponseBO idTokenResponse = AuthResponseBO.builder()
                .token("id token")
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .build();

        Mockito.when(basicAuth.authenticateAndGetAccount(authRequest))
                .thenReturn(CompletableFuture.completedFuture(account));

        Mockito.when(accessTokenProvider.generateToken(account, authRequest.getRestrictions(), options))
                .thenReturn(accessTokenResponse);

        Mockito.when(idTokenProvider.generateToken(account))
                .thenReturn(idTokenResponse);

        AuthResponseBO actual = basicToOIdC.exchange(authRequest).join();

        AuthResponseBO expected = AuthResponseBO.builder()
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .type("oidc")
                .token(OAuthResponseBO.builder()
                        .accessToken((String) accessTokenResponse.getToken())
                        .idToken((String) idTokenResponse.getToken())
                        .refreshToken((String) accessTokenResponse.getRefreshToken())
                        .build())
                .build();

        assertThat(actual).isEqualTo(expected);
    }
}