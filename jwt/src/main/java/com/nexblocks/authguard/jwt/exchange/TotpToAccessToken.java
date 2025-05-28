package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.totp.TotpVerifier;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import io.smallrye.mutiny.Uni;

@TokenExchange(from = "totp", to = "accessToken")
public class TotpToAccessToken implements Exchange {
    private final AccountsService accountsService;
    private final TotpVerifier totpVerifier;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public TotpToAccessToken(final AccountsService accountsService, final TotpVerifier otpVerifier,
                             final AccessTokenProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.totpVerifier = otpVerifier;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        // TODO ensure the options match
        return totpVerifier.verifyAndGetAccountTokenAsync(request)
                .flatMap(accountToken -> accountsService.getById(accountToken.getAssociatedAccountId(), request.getDomain())
                        .flatMap(opt -> {
                            if (opt.isEmpty()) {
                                return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                        "Failed to generate access token"));
                            }

                            return generate(opt.get(), accountToken, request);
                        }));
    }

    private Uni<AuthResponseBO> generate(final AccountBO account, final AccountTokenDO accountToken,
                                                       final AuthRequestBO request) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("otp")
                .userAgent(request.getUserAgent())
                .sourceIp(request.getSourceIp())
                .clientId(request.getClientId())
                .externalSessionId(request.getExternalSessionId())
                .deviceId(request.getDeviceId())
                .trackingSession(accountToken.getTrackingSession())
                .build();

        return accessTokenProvider.generateToken(account, options);
    }
}
