package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import io.smallrye.mutiny.Uni;

/**
 * Authentication service interface.
 */
public interface AuthenticationService {
    Uni<AuthResponseBO> authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    Uni<AuthResponseBO> logout(AuthRequestBO authRequest, RequestContextBO requestContext);

    Uni<AuthResponseBO> refresh(AuthRequestBO authRequest, RequestContextBO requestContext);
}
