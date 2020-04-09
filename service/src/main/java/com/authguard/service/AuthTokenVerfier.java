package com.authguard.service;

import java.util.Optional;

public interface AuthTokenVerfier {
    /**
     * Verify a given token.
     * @return The associated account ID or empty if the token was invalid.
     */
    Optional<String> verifyAccountToken(final String token);
}
