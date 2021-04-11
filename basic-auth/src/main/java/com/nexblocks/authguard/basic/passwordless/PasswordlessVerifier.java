package com.nexblocks.authguard.basic.passwordless;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.EntityType;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.time.OffsetDateTime;

public class PasswordlessVerifier implements AuthVerifier {
    private final AccountTokensRepository accountTokensRepository;

    @Inject
    public PasswordlessVerifier(final AccountTokensRepository accountTokensRepository) {
        this.accountTokensRepository = accountTokensRepository;
    }

    @Override
    public Either<Exception, String> verifyAccountToken(final String passwordlessToken) {
        return accountTokensRepository.getByToken(passwordlessToken)
                .join()
                .map(this::verifyToken)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN,
                        "No passwordless token found for " + passwordlessToken)));
    }

    private Either<Exception, String> verifyToken(final AccountTokenDO accountToken) {
        if (accountToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Expired passwordless token",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId()));
        }

        return Either.right(accountToken.getAssociatedAccountId());
    }
}
