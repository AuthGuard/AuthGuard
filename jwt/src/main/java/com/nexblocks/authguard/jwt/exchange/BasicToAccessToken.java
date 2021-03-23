package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;
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
                .map(account -> {
                    if (request.getRestrictions() == null) {
                        return accessTokenProvider.generateToken(account);
                    } else {
                        return accessTokenProvider.generateToken(account, request.getRestrictions());
                    }
                });
    }
}
