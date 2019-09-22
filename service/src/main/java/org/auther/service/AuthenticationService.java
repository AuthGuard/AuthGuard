package org.auther.service;

import org.auther.service.model.TokensBO;

import java.util.Optional;

/**
 * Authentication service interface.
 *
 * @see org.auther.service.impl.AuthenticationServiceImpl
 */
public interface AuthenticationService {
    /**
     * Authenticate a user using the value of an Authorization
     * header and generate a token.
     * @param authHeader Authorization header
     * @return An optional of the generated tokens or an
     * empty optional if the user was not found.
     * @throws org.auther.service.exceptions.ServiceAuthorizationException
     *         if anything was wrong in the header.
     */
    Optional<TokensBO> authenticate(String authHeader);
}
