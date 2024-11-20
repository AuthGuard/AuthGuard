package com.nexblocks.authguard.jwt;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.model.*;

import java.util.concurrent.CompletableFuture;

@ProvidesToken("oidc")
public class OpenIdConnectTokenProvider implements AuthProvider {
    private final AccessTokenProvider accessTokenProvider;
    private final IdTokenProvider idTokenProvider;

    @Inject
    public OpenIdConnectTokenProvider(final AccessTokenProvider accessTokenProvider,
                                      final IdTokenProvider idTokenProvider) {
        this.accessTokenProvider = accessTokenProvider;
        this.idTokenProvider = idTokenProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> generateToken(final AccountBO account) {
        return generateToken(account, null, null);
    }

    @Override
    public CompletableFuture<AuthResponseBO> generateToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
        return accessTokenProvider.generateToken(account, restrictions, options)
                .thenCompose(accessToken -> idTokenProvider.generateToken(account)
                        .thenApply(idToken -> AuthResponseBO.builder()
                                .type("oidc")
                                .entityId(account.getId())
                                .entityType(EntityType.ACCOUNT)
                                .token(OAuthResponseBO.builder()
                                        .accessToken((String) accessToken.getToken())
                                        .idToken((String) idToken.getToken())
                                        .refreshToken((String) accessToken.getRefreshToken())
                                        .build())
                                .validFor(accessToken.getValidFor())
                                .trackingSession(options.getTrackingSession())
                                .build()));
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("OpenID Connect tokens cannot be generated for applications");
    }
}
