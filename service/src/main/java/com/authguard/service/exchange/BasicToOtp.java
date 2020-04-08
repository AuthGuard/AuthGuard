package com.authguard.service.exchange;

import com.authguard.service.OtpService;
import com.authguard.service.exchange.helpers.BasicAuth;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "otp")
public class BasicToOtp implements Exchange {
    private final BasicAuth basicAuth;
    private final OtpService otpService;

    @Inject
    public BasicToOtp(final BasicAuth basicAuth, final OtpService otpService) {
        this.basicAuth = basicAuth;
        this.otpService = otpService;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(otpService::generate);
    }
}
