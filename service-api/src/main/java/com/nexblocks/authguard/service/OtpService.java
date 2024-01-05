package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.util.concurrent.CompletableFuture;

public interface OtpService {
    CompletableFuture<AuthResponseBO> authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    CompletableFuture<AuthResponseBO> authenticate(long passwordId, String otp, RequestContextBO requestContext);
}
