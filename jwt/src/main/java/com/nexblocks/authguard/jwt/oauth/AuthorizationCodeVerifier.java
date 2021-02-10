package com.nexblocks.authguard.jwt.oauth;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.EntityType;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.time.ZonedDateTime;

public class AuthorizationCodeVerifier implements AuthVerifier {
    private final AccountTokensRepository accountTokensRepository;

    @Inject
    public AuthorizationCodeVerifier(final AccountTokensRepository accountTokensRepository) {
        this.accountTokensRepository = accountTokensRepository;
    }

    @Override
    public Either<Exception, String> verifyAccountToken(final String token) {
        return verifyAndGetAccountToken(token)
                .map(AccountTokenDO::getAssociatedAccountId);
    }

    @Override
    public Either<Exception, AccountTokenDO> verifyAndGetAccountToken(final String token) {
        return accountTokensRepository.getByToken(token)
                .join()
                .map(this::verifyToken)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid authorization code " + token)));
    }

    private Either<Exception, AccountTokenDO> verifyToken(final AccountTokenDO accountToken) {
        if (accountToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "The authorization code has expired",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId());
        }

        return Either.right(accountToken);
    }
}
