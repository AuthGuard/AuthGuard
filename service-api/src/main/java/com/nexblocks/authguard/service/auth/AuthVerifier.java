package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import io.vavr.control.Either;

import java.util.concurrent.CompletableFuture;

public interface AuthVerifier {
    /**
     * Verify a given token.
     *
     * @return The associated account ID or empty if the token was invalid.
     */
    Long verifyAccountToken(final String token);

    default CompletableFuture<Long> verifyAccountTokenAsync(final String token) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default Either<Exception, AccountTokenDO> verifyAndGetAccountToken(final String token) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<AccountTokenDO> verifyAndGetAccountTokenAsync(final String token) {
        throw new UnsupportedOperationException();
    }
}
