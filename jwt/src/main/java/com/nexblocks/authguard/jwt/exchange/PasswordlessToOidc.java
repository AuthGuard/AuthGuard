package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwordless.PasswordlessVerifier;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "passwordless", to = "oidc")
public class PasswordlessToOidc implements Exchange {
    private final AccountsService accountsService;
    private final PasswordlessVerifier passwordlessVerifier;
    private final OpenIdConnectTokenProvider openIdConnectTokenProvider;

    @Inject
    public PasswordlessToOidc(final AccountsService accountsService,
                              final PasswordlessVerifier passwordlessVerifier,
                              final OpenIdConnectTokenProvider openIdConnectTokenProvider) {
        this.accountsService = accountsService;
        this.passwordlessVerifier = passwordlessVerifier;
        this.openIdConnectTokenProvider = openIdConnectTokenProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return passwordlessVerifier.verifyAccountTokenAsync(request.getToken())
                .thenCompose(accountsService::getById)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return CompletableFuture.failedFuture(
                                new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "The account associated with that token does not exist"));
                    }

                    return CompletableFuture.completedFuture(generate(opt.get()));
                });
    }

    private AuthResponseBO generate(final AccountBO account) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("passwordless")
                .build();

        return openIdConnectTokenProvider.generateToken(account, options);
    }
}
