package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
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

@TokenExchange(from = "otp", to = "oidc")
public class OtpToOidc implements Exchange {
    private final AccountsService accountsService;
    private final OtpVerifier otpVerifier;
    private final OpenIdConnectTokenProvider openIdConnectTokenProvider;

    @Inject
    public OtpToOidc(final AccountsService accountsService, final OtpVerifier otpVerifier,
                     final OpenIdConnectTokenProvider openIdConnectTokenProvider) {
        this.accountsService = accountsService;
        this.otpVerifier = otpVerifier;
        this.openIdConnectTokenProvider = openIdConnectTokenProvider;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return otpVerifier.verifyAccountTokenAsync(request)
                .flatMap(id -> accountsService.getById(id, request.getDomain()))
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "The account associated with that token does not exist"));
                    }

                    return generate(opt.get());
                });
    }

    private Uni<AuthResponseBO> generate(final AccountBO account) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("otp")
                .build();

        return openIdConnectTokenProvider.generateToken(account, options);
    }
}
