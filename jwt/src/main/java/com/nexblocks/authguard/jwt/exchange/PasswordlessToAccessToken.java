package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.basic.passwordless.PasswordlessVerifier;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;
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
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return passwordlessVerifier.verifyAccountToken(request.getToken())
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
