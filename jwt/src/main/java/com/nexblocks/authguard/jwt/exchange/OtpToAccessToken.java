package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.google.inject.Inject;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "otp", to = "accessToken")
public class OtpToAccessToken implements Exchange {
    private final AccountsService accountsService;
    private final OtpVerifier otpVerifier;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public OtpToAccessToken(final AccountsService accountsService, final OtpVerifier otpVerifier,
                            final AccessTokenProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.otpVerifier = otpVerifier;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return otpVerifier.verifyAccountTokenAsync(request.getToken())
                .thenCompose(accountsService::getById)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                "Failed to generate access token"));
                    }

                    return generate(opt.get(), request);
                });
    }

    private CompletableFuture<AuthResponseBO> generate(final AccountBO account, final AuthRequestBO request) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("otp")
                .userAgent(request.getUserAgent())
                .sourceIp(request.getSourceIp())
                .clientId(request.getClientId())
                .externalSessionId(request.getExternalSessionId())
                .deviceId(request.getDeviceId())
                .build();

        return accessTokenProvider.generateToken(account, options);
    }
}
