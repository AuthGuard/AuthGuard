package com.authguard.service.exchange;

import com.authguard.service.basic.BasicAuthProvider;
import com.authguard.service.jwt.AccessTokenProvider;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "accessToken")
public class BasicToAccessToken implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public BasicToAccessToken(final BasicAuthProvider basicAuth, final AccessTokenProvider accessTokenProvider) {
        this.basicAuth = basicAuth;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(accessTokenProvider::generateToken);
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken, final TokenRestrictionsBO restrictions) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(account -> accessTokenProvider.generateToken(account, restrictions));
    }
}
