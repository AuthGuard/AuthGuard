package com.authguard.jwt.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.jwt.IdTokenProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "idToken")
public class BasicToIdToken implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final IdTokenProvider idTokenProvider;

    @Inject
    public BasicToIdToken(final BasicAuthProvider basicAuth, final IdTokenProvider idTokenProvider) {
        this.basicAuth = basicAuth;
        this.idTokenProvider = idTokenProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(String basicToken) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(idTokenProvider::generateToken);
    }
}
