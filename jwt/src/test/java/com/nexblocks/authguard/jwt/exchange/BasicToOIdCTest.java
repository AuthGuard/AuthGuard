package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.jwt.IdTokenProvider;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;

class BasicToOIdCTest {
    private BasicAuthProvider basicAuth;
    private AccessTokenProvider accessTokenProvider;
    private IdTokenProvider idTokenProvider;

    private BasicToOIdC basicToOIdC;

    @BeforeEach
    void setup() {
        basicAuth = Mockito.mock(BasicAuthProvider.class);
        accessTokenProvider = Mockito.mock(AccessTokenProvider.class);
        idTokenProvider = Mockito.mock(IdTokenProvider.class);

        OpenIdConnectTokenProvider openIdConnectTokenProvider = new OpenIdConnectTokenProvider(accessTokenProvider, idTokenProvider);

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
                .trackingSession("tracking-token")
                .build();

        Mockito.when(basicAuth.authenticateAndGetAccountSession(authRequest))
                .thenReturn(Uni.createFrom().item(AccountSessionBO.builder()
                        .account(account)
                        .session(SessionBO.builder()
                                .sessionToken("tracking-token")
                                .build())
                        .build()));

        Mockito.when(accessTokenProvider.generateToken(account, authRequest.getRestrictions(), options))
                .thenReturn(Uni.createFrom().item(accessTokenResponse));

        Mockito.when(idTokenProvider.generateToken(account))
                .thenReturn(Uni.createFrom().item(idTokenResponse));

        AuthResponseBO actual = basicToOIdC.exchange(authRequest).subscribeAsCompletionStage().join();

        AuthResponseBO expected = AuthResponseBO.builder()
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .type("oidc")
                .token(OAuthResponseBO.builder()
                        .accessToken((String) accessTokenResponse.getToken())
                        .idToken((String) idTokenResponse.getToken())
                        .refreshToken((String) accessTokenResponse.getRefreshToken())
                        .build())
                .trackingSession("tracking-token")
                .build();

        assertThat(actual).isEqualTo(expected);
    }
}