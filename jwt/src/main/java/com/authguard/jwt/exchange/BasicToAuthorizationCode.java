package com.authguard.jwt.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "authorizationCode")
public class BasicToAuthorizationCode implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final AuthorizationCodeProvider authorizationCodeProvider;

    @Inject
    public BasicToAuthorizationCode(final BasicAuthProvider basicAuth, final AuthorizationCodeProvider authorizationCodeProvider) {
        this.basicAuth = basicAuth;
        this.authorizationCodeProvider = authorizationCodeProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basic) {
        return basicAuth.authenticateAndGetAccount(basic)
                .map(authorizationCodeProvider::generateToken);
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basic, final TokenRestrictionsBO restrictions) {
        return basicAuth.authenticateAndGetAccount(basic)
                .map(account -> authorizationCodeProvider.generateToken(account, restrictions));
    }
}
