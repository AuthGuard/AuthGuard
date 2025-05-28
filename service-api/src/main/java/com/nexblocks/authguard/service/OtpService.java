package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import io.smallrye.mutiny.Uni;

public interface OtpService {
    Uni<AuthResponseBO> authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    Uni<AuthResponseBO> authenticate(long passwordId, String otp, RequestContextBO requestContext);
}
