package com.authguard.service;

import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.impl.AuthenticationServiceImpl;
import com.authguard.service.model.TokensBO;

import java.util.Optional;

/**
 * Authentication service interface.
 *
 * @see AuthenticationServiceImpl
 */
public interface AuthenticationService {
    /**
     * Authenticate a user using the value of an Authorization
     * header and generate a token.
     * @param header Authorization header
     * @return An optional of the generated tokens or an
     * empty optional if the user was not found.
     * @throws ServiceAuthorizationException
     *         if anything was wrong in the header.
     */
    Optional<TokensBO> authenticate(String header);
}
