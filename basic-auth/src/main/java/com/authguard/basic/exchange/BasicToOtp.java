package com.authguard.basic.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.basic.otp.OtpProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

@TokenExchange(from = "basic", to = "otp")
public class BasicToOtp implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final OtpProvider otpProvider;

    @Inject
    public BasicToOtp(final BasicAuthProvider basicAuth, final OtpProvider otpProvider) {
        this.basicAuth = basicAuth;
        this.otpProvider = otpProvider;
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(otpProvider::generateToken);
    }
}
