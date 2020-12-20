package com.authguard.service;

import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;

import java.util.Optional;

/**
 * Authentication service interface.
 */
public interface AuthenticationService {
    Optional<TokensBO> authenticate(AuthRequestBO authRequest);
}
