package com.authguard.service.exchange;

import com.authguard.service.exchange.helpers.BasicAuth;
import com.authguard.service.jwt.AccessTokenProvider;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "accessToken")
public class BasicToAccessToken implements Exchange {
    private final BasicAuth basicAuth;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public BasicToAccessToken(final BasicAuth basicAuth, final AccessTokenProvider accessTokenProvider) {
        this.basicAuth = basicAuth;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(String basicToken) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(accessTokenProvider::generateToken);
    }
}
