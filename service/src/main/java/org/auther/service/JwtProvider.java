package org.auther.service;

import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public interface JwtProvider {
    /**
     * Generate a token and (optionally) a refresh token.
     * @return
     */
    TokensBO generateToken(AccountBO account);

    /**
     * Validate that a token is valid.
     * @param token
     * @return An empty optional to signal failure, an updated token in case of JTI, or the same token.
     */
    Optional<String> validateToken(String token);
}
