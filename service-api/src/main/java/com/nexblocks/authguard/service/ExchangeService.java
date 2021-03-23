package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.TokensBO;

public interface ExchangeService {
    TokensBO exchange(AuthRequestBO authRequest, String fromTokenType, String toTokenType, RequestContextBO requestContext);

    boolean supportsExchange(String fromTokenType, String toTokenType);

    TokensBO delete(AuthRequestBO authRequest, String tokenType);
}
