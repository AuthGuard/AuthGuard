package com.nexblocks.authguard.sessions.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwordless.PasswordlessVerifier;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import com.nexblocks.authguard.sessions.SessionProvider;

import io.smallrye.mutiny.Uni;

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
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("passwordless")
                .build();

        return passwordlessVerifier.verifyAccountTokenAsync(request)
                .flatMap(id -> accountsService.getById(id, request.getDomain()))
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "Account does not exist");
                    }

                    return sessionProvider.generateToken(opt.get(), options);
                });
    }
}
