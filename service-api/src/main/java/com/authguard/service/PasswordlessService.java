package com.authguard.service;

import com.authguard.service.model.TokensBO;

public interface PasswordlessService {
    TokensBO authenticate(String passwordlessToken);
}
