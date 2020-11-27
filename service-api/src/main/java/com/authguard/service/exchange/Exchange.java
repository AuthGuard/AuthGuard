package com.authguard.service.exchange;

import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import io.vavr.control.Either;

public interface Exchange {
    Either<Exception, TokensBO> exchangeToken(String fromToken);

    default Either<Exception, TokensBO> exchangeToken(String fromToken, TokenRestrictionsBO restrictions) {
        return exchangeToken(fromToken);
    }
}
