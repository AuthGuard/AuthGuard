package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;

import io.smallrye.mutiny.Uni;

public interface PasswordlessService {
    Uni<AuthResponseBO> authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    Uni<AuthResponseBO> authenticate(String passwordlessToken, RequestContextBO requestContext);
}
