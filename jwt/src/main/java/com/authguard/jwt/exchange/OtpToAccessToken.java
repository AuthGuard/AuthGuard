package com.authguard.jwt.exchange;

import com.authguard.basic.otp.OtpVerifier;
import com.authguard.jwt.AccessTokenProvider;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

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
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return otpVerifier.verifyAccountToken(request.getToken())
                .map(accountsService::getById)
                .flatMap(accountOpt -> accountOpt
                        .map(this::generate)
                        .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                "Failed to generate access token"))));
    }

    private Either<Exception, TokensBO> generate(final AccountBO account) {
        return Either.right(accessTokenProvider.generateToken(account));
    }
}
