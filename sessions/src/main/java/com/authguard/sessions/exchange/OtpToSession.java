package com.authguard.sessions.exchange;

import com.authguard.service.AccountsService;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import com.authguard.basic.otp.OtpVerifier;
import com.authguard.sessions.SessionProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "otp", to = "session")
public class OtpToSession implements Exchange {
    private final AccountsService accountsService;
    private final OtpVerifier otpVerifier;
    private final SessionProvider sessionProvider;

    @Inject
    public OtpToSession(final AccountsService accountsService, final OtpVerifier otpVerifier,
                        final SessionProvider sessionProvider) {
        this.accountsService = accountsService;
        this.otpVerifier = otpVerifier;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String otp) {
        return otpVerifier.verifyAccountToken(otp)
                .flatMap(accountsService::getById)
                .map(sessionProvider::generateToken);
    }
}
