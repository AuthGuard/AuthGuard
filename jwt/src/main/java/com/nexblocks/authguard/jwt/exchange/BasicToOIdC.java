package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import io.vavr.control.Either;

@TokenExchange(from = "basic", to = "oidc")
public class BasicToOIdC implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final OpenIdConnectTokenProvider openIdConnectTokenProvider;

    @Inject
    public BasicToOIdC(final BasicAuthProvider basicAuth,
                       final OpenIdConnectTokenProvider openIdConnectTokenProvider) {
        this.basicAuth = basicAuth;
        this.openIdConnectTokenProvider = openIdConnectTokenProvider;
    }

    @Override
    public Either<Exception, AuthResponseBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(account -> generateTokens(account, request.getRestrictions()));
    }

    private AuthResponseBO generateTokens(final AccountBO account, final TokenRestrictionsBO restrictions) {
        return openIdConnectTokenProvider.generateToken(account, restrictions);
    }
}
