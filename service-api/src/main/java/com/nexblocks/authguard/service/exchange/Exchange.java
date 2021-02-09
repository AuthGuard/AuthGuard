package com.nexblocks.authguard.service.exchange;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import com.nexblocks.authguard.service.model.TokensBO;
import io.vavr.control.Either;

public interface Exchange {
    Either<Exception, TokensBO> exchange(AuthRequestBO request);

    default Either<Exception, TokensBO> exchange(AuthRequestBO request, TokenRestrictionsBO restrictions) {
        return exchange(request);
    }
}
