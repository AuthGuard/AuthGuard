package com.nexblocks.authguard.jwt.oauth;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AuthRequest;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.time.Instant;
import io.smallrye.mutiny.Uni;

public class AuthorizationCodeVerifier implements AuthVerifier {
    private final AccountTokensRepository accountTokensRepository;

    @Inject
    public AuthorizationCodeVerifier(final AccountTokensRepository accountTokensRepository) {
        this.accountTokensRepository = accountTokensRepository;
    }

    @Override
    public Long verifyAccountToken(final String token) {
        return verifyAndGetAccountToken(AuthRequestBO.builder().token(token).build())
                .map(AccountTokenDO::getAssociatedAccountId)
                .getOrElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid authorization code"));
    }

    @Override
    public Either<Exception, AccountTokenDO> verifyAndGetAccountToken(final AuthRequest request) {
        return accountTokensRepository.getByToken(request.getToken())
                .subscribeAsCompletionStage()
                .join()
                .map(this::verifyToken)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid authorization code")));
    }

    @Override
    public Uni<Long> verifyAccountTokenAsync(final AuthRequest request) {
        return verifyAndGetAccountTokenAsync(request)
                .map(AccountTokenDO::getAssociatedAccountId);
    }

    @Override
    public Uni<AccountTokenDO> verifyAndGetAccountTokenAsync(final AuthRequest request) {
        return accountTokensRepository.getByToken(request.getToken())
                .flatMap(opt -> {
                    if (opt.isPresent()) {
                        return AsyncUtils.uniFromTry(tryVerifyToken(opt.get()));
                    }

                    return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN, "Invalid expired or invalid"));
                });
    }

    private Either<Exception, AccountTokenDO> verifyToken(final AccountTokenDO accountToken) {
        if (accountToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Expired authorization code",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId());
        }

        return Either.right(accountToken);
    }

    private Try<AccountTokenDO> tryVerifyToken(final AccountTokenDO accountToken) {
        if (accountToken.getExpiresAt().isBefore(Instant.now())) {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN,
                    "The authorization code has expired",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId()));
        }

        return Try.success(accountToken);
    }
}
