package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.vavr.control.Try;

public interface ActionTokenService {
    Try<AuthResponseBO> generateOtp(String accountId);
    Try<ActionTokenBO> generateFromBasicAuth(AuthRequestBO authRequest, String action);
    Try<ActionTokenBO> generateFromOtp(String passwordId, String otp, String action);
    Try<ActionTokenBO> verifyToken(String token, String action);
}
