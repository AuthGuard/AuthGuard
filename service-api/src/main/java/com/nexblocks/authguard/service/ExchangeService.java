package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import com.nexblocks.authguard.service.model.TokensBO;

public interface ExchangeService {
    TokensBO exchange(AuthRequestBO authRequest, String fromTokenType, String toTokenType);

    TokensBO exchange(AuthRequestBO authRequest, TokenRestrictionsBO restrictions, String fromTokenType, String toTokenType) ;

    boolean supportsExchange(String fromTokenType, String toTokenType);

    TokensBO delete(AuthRequestBO authRequest, String tokenType);
}
