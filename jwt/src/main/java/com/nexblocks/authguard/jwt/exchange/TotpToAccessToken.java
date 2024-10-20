package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.totp.TotpVerifier;
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

import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        // TODO ensure the options match
        return totpVerifier.verifyAndGetAccountTokenAsync(request)
                .thenCompose(accountToken ->
                        accountsService.getById(accountToken.getAssociatedAccountId(), request.getDomain()))
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
