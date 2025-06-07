package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.model.AuthRequest;
import io.vavr.control.Either;

import io.smallrye.mutiny.Uni;

public interface AuthVerifier {
    Uni<Long> verifyAccountToken(final String token);

    default Uni<Long> verifyAccountTokenAsync(final AuthRequest request) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default Either<Exception, AccountTokenDO> verifyAndGetAccountToken(final AuthRequest request) {
        throw new UnsupportedOperationException();
    }

    default Uni<AccountTokenDO> verifyAndGetAccountTokenAsync(final AuthRequest request) {
        throw new UnsupportedOperationException();
    }
}
