package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;

public interface OtpService {
    TokensBO authenticate(AuthRequestBO authRequest);

    TokensBO authenticate(String passwordId, String otp);
}
