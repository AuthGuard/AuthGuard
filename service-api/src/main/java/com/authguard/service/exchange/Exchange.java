package com.authguard.service.exchange;

import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import io.vavr.control.Either;

public interface Exchange {
    Either<Exception, TokensBO> exchange(AuthRequestBO request);

    default Either<Exception, TokensBO> exchange(AuthRequestBO request, TokenRestrictionsBO restrictions) {
        return exchange(request);
    }
}
