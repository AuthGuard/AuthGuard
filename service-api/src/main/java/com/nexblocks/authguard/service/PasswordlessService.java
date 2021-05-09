package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;

public interface PasswordlessService {
    AuthResponseBO authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    AuthResponseBO authenticate(String passwordlessToken, RequestContextBO requestContext);
}
