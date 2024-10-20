package com.nexblocks.authguard.sessions.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import com.nexblocks.authguard.sessions.SessionProvider;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "otp", to = "sessionToken")
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
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("otp")
                .build();

        return otpVerifier.verifyAccountTokenAsync(request)
                .thenCompose(id -> accountsService.getById(id, request.getDomain()))
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        throw  new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "Account does not exist");
                    }

                    return sessionProvider.generateToken(opt.get(), options);
                });
    }
}
