package com.nexblocks.authguard.sessions.exchange;

import com.nexblocks.authguard.basic.passwordless.PasswordlessVerifier;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import com.nexblocks.authguard.sessions.SessionProvider;
import com.google.inject.Inject;
import io.vavr.control.Either;

@TokenExchange(from = "passwordless", to = "sessionToken")
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
    public Either<Exception, AuthResponseBO> exchange(final AuthRequestBO request) {
        final TokenOptionsBO options = TokenOptionsBO.builder()
                .source("passwordless")
                .build();

        return passwordlessVerifier.verifyAccountToken(request.getToken())
                .flatMap(this::getAccount)
                .map(account -> sessionProvider.generateToken(account, options));
    }

    private Either<Exception, AccountBO> getAccount(final long accountId) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Account " + accountId + " does not exist")));
    }
}
