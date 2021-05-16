package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.util.Optional;

/**
 * Authentication service interface.
 */
public interface AuthenticationService {
    Optional<AuthResponseBO> authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    Optional<AuthResponseBO> logout(AuthRequestBO authRequest, RequestContextBO requestContext);
}
