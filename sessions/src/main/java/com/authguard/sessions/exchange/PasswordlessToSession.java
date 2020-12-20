package com.authguard.sessions.exchange;

import com.authguard.basic.passwordless.PasswordlessVerifier;
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

@TokenExchange(from = "passwordless", to = "session")
public class PasswordlessToSession implements Exchange {
    private final AccountsService accountsService;
    private final PasswordlessVerifier passwordlessVerifier;
    private final SessionProvider sessionProvider;

    @Inject
    public PasswordlessToSession(final AccountsService accountsService,
                                 final PasswordlessVerifier passwordlessVerifier,
                                 final SessionProvider sessionProvider) {
        this.accountsService = accountsService;
        this.passwordlessVerifier = passwordlessVerifier;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return passwordlessVerifier.verifyAccountToken(request.getToken())
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
