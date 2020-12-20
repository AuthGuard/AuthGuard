package com.authguard.jwt.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.jwt.IdTokenProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

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
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(idTokenProvider::generateToken);
    }
}
