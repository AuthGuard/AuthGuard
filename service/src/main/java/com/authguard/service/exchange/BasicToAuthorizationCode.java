package com.authguard.service.exchange;

import com.authguard.service.exchange.helpers.BasicAuth;
import com.authguard.service.model.TokensBO;
import com.authguard.service.oauth.AuthorizationCodeProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "authorizationCode")
public class BasicToAuthorizationCode implements Exchange {
    private final BasicAuth basicAuth;
    private final AuthorizationCodeProvider authorizationCodeProvider;

    @Inject
    public BasicToAuthorizationCode(final BasicAuth basicAuth, final AuthorizationCodeProvider authorizationCodeProvider) {
        this.basicAuth = basicAuth;
        this.authorizationCodeProvider = authorizationCodeProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basic) {
        return basicAuth.authenticateAndGetAccount(basic)
                .map(authorizationCodeProvider::generateToken);
    }
}
