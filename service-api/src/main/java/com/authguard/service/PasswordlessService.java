package com.authguard.service;

import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;

public interface PasswordlessService {
    TokensBO authenticate(AuthRequestBO authRequest);

    TokensBO authenticate(String passwordlessToken);
}
