package com.authguard.jwt.exchange;

import com.authguard.basic.passwordless.PasswordlessVerifier;
import com.authguard.jwt.AccessTokenProvider;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

@TokenExchange(from = "passwordless", to = "accessToken")
public class PasswordlessToAccessToken implements Exchange {
    private final AccountsService accountsService;
    private final PasswordlessVerifier passwordlessVerifier;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public PasswordlessToAccessToken(final AccountsService accountsService,
                                     final PasswordlessVerifier passwordlessVerifier,
                                     final AccessTokenProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.passwordlessVerifier = passwordlessVerifier;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Either<Exception, TokensBO> exchangeToken(final String passwordlessToken) {
        return passwordlessVerifier.verifyAccountToken(passwordlessToken)
                .map(accountsService::getById)
                .flatMap(accountOpt -> accountOpt
                        .map(this::generate)
                        .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                "Failed to generate access token"))));
    }

    private Either<Exception, TokensBO> generate(final AccountBO account) {
        return Either.right(accessTokenProvider.generateToken(account));
    }
}
