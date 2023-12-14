package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import io.vavr.control.Either;

public interface AuthVerifier {
    /**
     * Verify a given token.
     *
     * @return The associated account ID or empty if the token was invalid.
     */
    Either<Exception, Long> verifyAccountToken(final String token);

    default Either<Exception, AccountTokenDO> verifyAndGetAccountToken(final String token) {
        throw new UnsupportedOperationException();
    }
}
