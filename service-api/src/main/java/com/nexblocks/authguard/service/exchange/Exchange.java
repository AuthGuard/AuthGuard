package com.nexblocks.authguard.service.exchange;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.vavr.control.Either;

public interface Exchange {
    Either<Exception, AuthResponseBO> exchange(AuthRequestBO request);
}
