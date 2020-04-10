package com.authguard.service.exchange;

import com.authguard.service.exchange.helpers.BasicAuth;
import com.authguard.service.model.TokensBO;
import com.authguard.service.otp.OtpProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "otp")
public class BasicToOtp implements Exchange {
    private final BasicAuth basicAuth;
    private final OtpProvider otpProvider;

    @Inject
    public BasicToOtp(final BasicAuth basicAuth, final OtpProvider otpProvider) {
        this.basicAuth = basicAuth;
        this.otpProvider = otpProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(otpProvider::generateToken);
    }
}
