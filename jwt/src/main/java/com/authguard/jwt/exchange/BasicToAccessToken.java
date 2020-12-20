package com.authguard.jwt.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.jwt.AccessTokenProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

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
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(accessTokenProvider::generateToken);
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request, final TokenRestrictionsBO restrictions) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(account -> accessTokenProvider.generateToken(account, restrictions));
    }
}
