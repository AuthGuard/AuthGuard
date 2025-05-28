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
import io.vavr.control.Try;

import io.smallrye.mutiny.Uni;

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
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return authorizationCodeVerifier.verifyAndGetAccountTokenAsync(request)
                .flatMap(accountToken -> generateToken(accountToken, request));
    }

    private Uni<AuthResponseBO> generateToken(final AccountTokenDO accountToken, final AuthRequestBO request) {
        TokenRestrictionsBO restrictions = getRestrictions(accountToken);
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("authorizationCode")
                .clientId(accountToken.getClientId())
                .deviceId(accountToken.getDeviceId())
                .userAgent(accountToken.getUserAgent())
                .externalSessionId(accountToken.getExternalSessionId())
                .sourceIp(accountToken.getSourceIp())
                .trackingSession(accountToken.getTrackingSession())
                .build();

        Try<Boolean> verificationResult = PkceVerifier.verifyIfPkce(accountToken, request);

        if (verificationResult.isFailure()) {
            return Uni.createFrom().failure(verificationResult.getCause());
        }

        return accountsServiceAdapter.getAccount(accountToken.getAssociatedAccountId())
                .flatMap(account -> openIdConnectTokenProvider.generateToken(account, restrictions, options));
    }

    private TokenRestrictionsBO getRestrictions(final AccountTokenDO accountToken) {
        return accountToken.getTokenRestrictions() == null
                ? null
                : serviceMapper.toBO(accountToken.getTokenRestrictions());
    }
}
