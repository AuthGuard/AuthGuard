package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.util.concurrent.CompletableFuture;

public interface ExchangeService {
    CompletableFuture<AuthResponseBO> exchange(AuthRequestBO authRequest, String fromTokenType, String toTokenType, RequestContextBO requestContext);

    boolean supportsExchange(String fromTokenType, String toTokenType);

    CompletableFuture<AuthResponseBO> delete(AuthRequestBO authRequest, String tokenType);
}
