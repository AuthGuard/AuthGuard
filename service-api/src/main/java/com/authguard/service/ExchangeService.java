package com.authguard.service;

import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;

public interface ExchangeService {
    TokensBO exchange(AuthRequestBO authRequest, String fromTokenType, String toTokenType);

    TokensBO exchange(AuthRequestBO authRequest, TokenRestrictionsBO restrictions, String fromTokenType, String toTokenType) ;

    boolean supportsExchange(String fromTokenType, String toTokenType);
}
