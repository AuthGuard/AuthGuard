package com.authguard.service;

import com.authguard.service.model.TokensBO;

public interface OtpService {
    TokensBO authenticate(String passwordId, String otp);
}
