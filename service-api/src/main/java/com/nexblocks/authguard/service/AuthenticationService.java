package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.util.concurrent.CompletableFuture;

/**
 * Authentication service interface.
 */
public interface AuthenticationService {
    CompletableFuture<AuthResponseBO> authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    CompletableFuture<AuthResponseBO> logout(AuthRequestBO authRequest, RequestContextBO requestContext);

    CompletableFuture<AuthResponseBO> refresh(AuthRequestBO authRequest, RequestContextBO requestContext);
}
