package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.model.AuthRequest;
import io.vavr.control.Either;

import java.util.concurrent.CompletableFuture;

public interface AuthVerifier {
    Long verifyAccountToken(final String token);

    default CompletableFuture<Long> verifyAccountTokenAsync(final AuthRequest request) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default Either<Exception, AccountTokenDO> verifyAndGetAccountToken(final AuthRequest request) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<AccountTokenDO> verifyAndGetAccountTokenAsync(final AuthRequest request) {
        throw new UnsupportedOperationException();
    }
}
