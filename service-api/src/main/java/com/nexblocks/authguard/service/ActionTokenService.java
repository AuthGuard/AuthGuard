package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.util.concurrent.CompletableFuture;

public interface ActionTokenService {
    CompletableFuture<AuthResponseBO> generateOtp(long accountId);
    CompletableFuture<ActionTokenBO> generateFromBasicAuth(AuthRequestBO authRequest, String action);
    CompletableFuture<ActionTokenBO> generateFromOtp(long passwordId, String otp, String action);
    CompletableFuture<ActionTokenBO> verifyToken(String token, String action);
}
