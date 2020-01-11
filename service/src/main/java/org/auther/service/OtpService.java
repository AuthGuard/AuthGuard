package org.auther.service;

import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

public interface OtpService {
    TokensBO generate(AccountBO account);

    TokensBO authenticate(String passwordId, String otp);
}
