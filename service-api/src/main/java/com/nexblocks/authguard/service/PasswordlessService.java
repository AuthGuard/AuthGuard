package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;

import java.util.concurrent.CompletableFuture;

public interface PasswordlessService {
    CompletableFuture<AuthResponseBO> authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    CompletableFuture<AuthResponseBO> authenticate(String passwordlessToken, RequestContextBO requestContext);
}
