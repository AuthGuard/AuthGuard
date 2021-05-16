package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeVerifier;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import io.vavr.control.Either;

@TokenExchange(from = "authorizationCode", to = "oidc")
public class AuthorizationCodeToOidc implements Exchange {
    private final AccountsServiceAdapter accountsServiceAdapter;
    private final AuthorizationCodeVerifier authorizationCodeVerifier;
    private final OpenIdConnectTokenProvider openIdConnectTokenProvider;
    private final ServiceMapper serviceMapper;

    public AuthorizationCodeToOidc(final AccountsServiceAdapter accountsServiceAdapter,
                                   final AuthorizationCodeVerifier authorizationCodeVerifier,
                                   final OpenIdConnectTokenProvider openIdConnectTokenProvider,
                                   final ServiceMapper serviceMapper) {
        this.accountsServiceAdapter = accountsServiceAdapter;
        this.authorizationCodeVerifier = authorizationCodeVerifier;
        this.openIdConnectTokenProvider = openIdConnectTokenProvider;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Either<Exception, AuthResponseBO> exchange(final AuthRequestBO request) {
        return authorizationCodeVerifier.verifyAndGetAccountToken(request.getToken())
                .flatMap(this::generateToken);
    }

    private Either<Exception, AuthResponseBO> generateToken(final AccountTokenDO accountToken) {
        final TokenRestrictionsBO restrictions = getRestrictions(accountToken);

        return accountsServiceAdapter.getAccount(accountToken.getAssociatedAccountId())
                .map(account -> openIdConnectTokenProvider.generateToken(account, restrictions));
    }

    private TokenRestrictionsBO getRestrictions(final AccountTokenDO accountToken) {
        return accountToken.getTokenRestrictions() == null
                ? null
                : serviceMapper.toBO(accountToken.getTokenRestrictions());
    }
}
