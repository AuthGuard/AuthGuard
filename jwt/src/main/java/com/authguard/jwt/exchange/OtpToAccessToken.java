package com.authguard.jwt.exchange;

import com.authguard.service.AccountsService;
import com.authguard.jwt.AccessTokenProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import com.authguard.basic.otp.OtpVerifier;
import com.google.inject.Inject;

import java.util.Optional;

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
    public Optional<TokensBO> exchangeToken(final String otpToken) {
        return otpVerifier.verifyAccountToken(otpToken)
                .flatMap(accountsService::getById)
                .map(accessTokenProvider::generateToken);
    }
}
