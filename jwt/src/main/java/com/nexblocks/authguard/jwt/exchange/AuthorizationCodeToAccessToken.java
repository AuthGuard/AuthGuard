package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeVerifier;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.TokenOptionsMapper;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import io.vavr.control.Try;

import io.smallrye.mutiny.Uni;

@TokenExchange(from = "authorizationCode", to = "accessToken")
public class AuthorizationCodeToAccessToken implements Exchange {
    private final AccountsServiceAdapter accountsServiceAdapter;
    private final AuthorizationCodeVerifier authorizationCodeVerifier;
    private final AccessTokenProvider accessTokenProvider;
    private final ServiceMapper serviceMapper;

    @Inject
    public AuthorizationCodeToAccessToken(final AccountsServiceAdapter accountsServiceAdapter,
                                          final AuthorizationCodeVerifier authorizationCodeVerifier,
                                          final AccessTokenProvider accessTokenProvider,
                                          final ServiceMapper serviceMapper) {
        this.accountsServiceAdapter = accountsServiceAdapter;
        this.authorizationCodeVerifier = authorizationCodeVerifier;
        this.accessTokenProvider = accessTokenProvider;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return authorizationCodeVerifier.verifyAndGetAccountTokenAsync(request)
                .flatMap(accountToken -> generateToken(accountToken, request));
    }

    private Uni<AuthResponseBO> generateToken(AccountTokenDO accountToken, AuthRequestBO request) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source(accountToken.getSourceAuthType())
                .clientId(accountToken.getClientId())
                .deviceId(accountToken.getDeviceId())
                .userAgent(accountToken.getUserAgent())
                .externalSessionId(accountToken.getExternalSessionId())
                .sourceIp(accountToken.getSourceIp())
                .trackingSession(accountToken.getTrackingSession())
                .build();

        TokenRestrictionsBO restrictions = getRestrictions(accountToken);
        Try<Boolean> verificationResult = PkceVerifier.verifyIfPkce(accountToken, request);

        if (verificationResult.isFailure()) {
            return Uni.createFrom().failure(verificationResult.getCause());
        }

        return accountsServiceAdapter.getAccount(accountToken.getAssociatedAccountId())
                .flatMap(account ->  accessTokenProvider.generateToken(account, restrictions, options));
    }

    private TokenRestrictionsBO getRestrictions(final AccountTokenDO accountToken) {
        return accountToken.getTokenRestrictions() == null
                ? null
                : serviceMapper.toBO(accountToken.getTokenRestrictions());
    }
}
