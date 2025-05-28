package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import io.smallrye.mutiny.Uni;

public interface ActionTokenService {
    Uni<AuthResponseBO> generateOtp(long accountId, String domain);
    Uni<ActionTokenBO> generateFromBasicAuth(AuthRequestBO authRequest, String action);
    Uni<ActionTokenBO> generateFromOtp(long passwordId, String domain, String otp, String action);
    Uni<ActionTokenBO> verifyToken(String token, String action);
}
