package com.authguard.service;

import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;

public interface OtpService {
    TokensBO generate(AccountBO account);

    TokensBO authenticate(String passwordId, String otp);
}
