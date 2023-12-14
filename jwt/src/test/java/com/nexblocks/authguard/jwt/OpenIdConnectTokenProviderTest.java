package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        final AccountBO account = AccountBO.builder().id(101).build();

        final AuthResponseBO accessTokenResponse = AuthResponseBO.builder()
                .token("access token")
                .refreshToken("refresh token")
                .build();

        final AuthResponseBO idTokenResponse = AuthResponseBO.builder()
                .token("id token")
                .build();

        Mockito.when(accessTokenProvider.generateToken(account, null, null))
                .thenReturn(accessTokenResponse);

        Mockito.when(idTokenProvider.generateToken(account))
                .thenReturn(idTokenResponse);

        final AuthResponseBO actual = openIdConnectTokenProvider.generateToken(account);

        final AuthResponseBO expected = AuthResponseBO.builder()
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
    void generateTokenWithRestrictions() {
        final AccountBO account = AccountBO.builder().id(101).build();

        final TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder()
                .addPermissions("permission")
                .build();

        final AuthResponseBO accessTokenResponse = AuthResponseBO.builder()
                .token("access token")
                .refreshToken("refresh token")
                .build();

        final AuthResponseBO idTokenResponse = AuthResponseBO.builder()
                .token("id token")
                .build();

        final TokenOptionsBO options = TokenOptionsBO.builder()
                .source("source")
                .build();

        Mockito.when(accessTokenProvider.generateToken(account, restrictions, options))
                .thenReturn(accessTokenResponse);

        Mockito.when(idTokenProvider.generateToken(account))
                .thenReturn(idTokenResponse);

        final AuthResponseBO actual = openIdConnectTokenProvider.generateToken(account, restrictions, options);

        final AuthResponseBO expected = AuthResponseBO.builder()
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
        final AppBO app = AppBO.builder().build();

        assertThatThrownBy(() -> openIdConnectTokenProvider.generateToken(app))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}