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

import io.smallrye.mutiny.Uni;

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
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return passwordlessVerifier.verifyAccountTokenAsync(request)
                .flatMap(id -> accountsService.getById(id, request.getDomain()))
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "The account associated with that token does not exist"));
                    }

                    return generate(opt.get());
                });
    }

    private Uni<AuthResponseBO> generate(final AccountBO account) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("passwordless")
                .build();

        return openIdConnectTokenProvider.generateToken(account, options);
    }
}
