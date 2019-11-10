package org.auther.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

/**
 * JWT interface.
 */
public interface JwtProvider {
    /**
     * Generate a token and (optionally) a refresh token.
     * @param account The account for which the tokens
     *                will be generated.
     * @return The generated tokens.
     */
    TokensBO generateToken(AccountBO account);

    /**
     * Validate that a token is valid.
     * @param token The JWT.
     * @return An empty optional to signal failure, or the token decoded.
     */
    Optional<DecodedJWT> validateToken(String token);
}
