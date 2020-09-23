package com.authguard.service.auth;

import com.authguard.dal.model.AccountTokenDO;

import java.util.Optional;

public interface AuthTokenVerfier {
    /**
     * Verify a given token.
     * @return The associated account ID or empty if the token was invalid.
     */
    Optional<String> verifyAccountToken(final String token);

    default Optional<AccountTokenDO> verifyAndGetAccountToken(final String token) {
        throw new UnsupportedOperationException();
    }
}
