package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.TokensBO;

public interface PasswordlessService {
    TokensBO authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    TokensBO authenticate(String passwordlessToken, RequestContextBO requestContext);
}
