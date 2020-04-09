package com.authguard.service.exchange;

import com.authguard.service.model.TokensBO;

import java.util.Optional;

public interface Exchange {
    Optional<TokensBO> exchangeToken(String fromToken);
}
