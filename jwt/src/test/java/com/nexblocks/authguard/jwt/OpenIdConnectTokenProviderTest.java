package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenIdConnectTokenProviderTest {
    private AccessTokenProvider accessTokenProvider;
    private IdTokenProvider idTokenProvider;

    private OpenIdConnectTokenProvider openIdConnectTokenProvider;

    @BeforeEach
    void setup() {
        accessTokenProvider = Mockito.mock(AccessTokenProvider.class);
        idTokenProvider = Mockito.mock(IdTokenProvider.class);

        openIdConnectTokenProvider = new OpenIdConnectTokenProvider(accessTokenProvider, idTokenProvider);
    }

    @Test
    void generateToken() {
        AccountBO account = AccountBO.builder().id(101).build();

        AuthResponseBO accessTokenResponse = AuthResponseBO.builder()
                .token("access token")
                .refreshToken("refresh token")
                .build();

        AuthResponseBO idTokenResponse = AuthResponseBO.builder()
                .token("id token")
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .trackingSession("tracking-session")
                .build();

        Mockito.when(accessTokenProvider.generateToken(account, null, options))
                .thenReturn(CompletableFuture.completedFuture(accessTokenResponse));

        Mockito.when(idTokenProvider.generateToken(account))
                .thenReturn(CompletableFuture.completedFuture(idTokenResponse));

        AuthResponseBO actual = openIdConnectTokenProvider.generateToken(account, options).join();

        AuthResponseBO expected = AuthResponseBO.builder()
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .type("oidc")
                .token(OAuthResponseBO.builder()
                        .accessToken((String) accessTokenResponse.getToken())
                        .idToken((String) idTokenResponse.getToken())
                        .refreshToken((String) accessTokenResponse.getRefreshToken())
                        .build())
                .trackingSession("tracking-session")
                .build();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void generateTokenWithRestrictions() {
        AccountBO account = AccountBO.builder().id(101).build();

        TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder()
                .addPermissions("permission")
                .build();

        AuthResponseBO accessTokenResponse = AuthResponseBO.builder()
                .token("access token")
                .refreshToken("refresh token")
                .build();

        AuthResponseBO idTokenResponse = AuthResponseBO.builder()
                .token("id token")
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("source")
                .build();

        Mockito.when(accessTokenProvider.generateToken(account, restrictions, options))
                .thenReturn(CompletableFuture.completedFuture(accessTokenResponse));

        Mockito.when(idTokenProvider.generateToken(account))
                .thenReturn(CompletableFuture.completedFuture(idTokenResponse));

        AuthResponseBO actual = openIdConnectTokenProvider.generateToken(account, restrictions, options).join();

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

    @Test
    void generateTokenApps() {
        AppBO app = AppBO.builder().build();

        assertThatThrownBy(() -> openIdConnectTokenProvider.generateToken(app))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}