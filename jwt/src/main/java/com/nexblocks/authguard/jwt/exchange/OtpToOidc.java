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
import io.vavr.control.Either;

@TokenExchange(from = "passwordless", to = "oidc")
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
    public Either<Exception, AuthResponseBO> exchange(final AuthRequestBO request) {
        return otpVerifier.verifyAccountToken(request.getToken())
                .map(accountsService::getById)
                .flatMap(accountOpt -> accountOpt
                        .map(this::generate)
                        .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                "Failed to generate access token"))));
    }

    private Either<Exception, AuthResponseBO> generate(final AccountBO account) {
        return Either.right(openIdConnectTokenProvider.generateToken(account));
    }
}
