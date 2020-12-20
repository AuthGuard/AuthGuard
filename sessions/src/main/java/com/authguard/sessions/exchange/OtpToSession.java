package com.authguard.sessions.exchange;

import com.authguard.basic.otp.OtpVerifier;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import com.authguard.sessions.SessionProvider;
import com.google.inject.Inject;
import io.vavr.control.Either;

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
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return otpVerifier.verifyAccountToken(request.getToken())
                .flatMap(this::getAccount)
                .map(sessionProvider::generateToken);
    }

    private Either<Exception, AccountBO> getAccount(final String accountId) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Account " + accountId + " does not exist")));
    }

    private Either<Exception, TokensBO> generate(final AccountBO account) {
        return Either.right(sessionProvider.generateToken(account));
    }
}
