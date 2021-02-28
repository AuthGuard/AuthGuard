package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;

import java.util.Optional;

/**
 * Authentication service interface.
 */
public interface AuthenticationService {
    Optional<TokensBO> authenticate(AuthRequestBO authRequest);

    Optional<TokensBO> logout(AuthRequestBO authRequest);
}
