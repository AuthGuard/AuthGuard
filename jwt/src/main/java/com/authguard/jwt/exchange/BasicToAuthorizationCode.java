package com.authguard.jwt.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

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
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(authorizationCodeProvider::generateToken);
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request, final TokenRestrictionsBO restrictions) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(account -> authorizationCodeProvider.generateToken(account, restrictions));
    }
}
