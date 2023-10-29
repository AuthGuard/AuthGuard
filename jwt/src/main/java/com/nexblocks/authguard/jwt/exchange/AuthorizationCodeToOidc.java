package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeVerifier;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "authorizationCode", to = "oidc")
public class AuthorizationCodeToOidc implements Exchange {
    private final AccountsServiceAdapter accountsServiceAdapter;
    private final AuthorizationCodeVerifier authorizationCodeVerifier;
    private final OpenIdConnectTokenProvider openIdConnectTokenProvider;
    private final ServiceMapper serviceMapper;

    @Inject
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
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return authorizationCodeVerifier.verifyAndGetAccountTokenAsync(request.getToken())
                .thenCompose(this::generateToken);
    }

    private CompletableFuture<AuthResponseBO> generateToken(final AccountTokenDO accountToken) {
        TokenRestrictionsBO restrictions = getRestrictions(accountToken);
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("authorizationCode")
                .clientId(accountToken.getClientId())
                .deviceId(accountToken.getDeviceId())
                .userAgent(accountToken.getUserAgent())
                .externalSessionId(accountToken.getExternalSessionId())
                .sourceIp(accountToken.getSourceIp())
                .build();

        return accountsServiceAdapter.getAccount(accountToken.getAssociatedAccountId())
                .thenCompose(account -> openIdConnectTokenProvider.generateToken(account, restrictions, options));
    }

    private TokenRestrictionsBO getRestrictions(final AccountTokenDO accountToken) {
        return accountToken.getTokenRestrictions() == null
                ? null
                : serviceMapper.toBO(accountToken.getTokenRestrictions());
    }
}
