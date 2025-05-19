package com.nexblocks.authguard.basic.passwordless;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AuthRequest;
import com.nexblocks.authguard.service.model.EntityType;
import com.google.inject.Inject;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Try;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class PasswordlessVerifier implements AuthVerifier {
    private final AccountTokensRepository accountTokensRepository;

    @Inject
    public PasswordlessVerifier(final AccountTokensRepository accountTokensRepository) {
        this.accountTokensRepository = accountTokensRepository;
    }

    @Override
    public Long verifyAccountToken(final String passwordlessToken) {
        throw new UnsupportedOperationException("Use the async variant");
    }

    @Override
    public CompletableFuture<Long> verifyAccountTokenAsync(final AuthRequest request) {
        return accountTokensRepository.getByToken(request.getToken())
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN,
                                "Passwordless token doesn't exist"));
                    }

                    return AsyncUtils.uniFromTry(verifyToken(opt.get()));
                })
                .subscribeAsCompletionStage();
    }

    private Try<Long> verifyToken(final AccountTokenDO accountToken) {
        if (accountToken.getExpiresAt().isBefore(Instant.now())) {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Expired passwordless token",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId()));
        }

        return Try.success(accountToken.getAssociatedAccountId());
    }
}
