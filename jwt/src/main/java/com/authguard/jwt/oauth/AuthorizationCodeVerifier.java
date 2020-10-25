package com.authguard.jwt.oauth;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.auth.AuthTokenVerfier;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.google.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Optional;

public class AuthorizationCodeVerifier implements AuthTokenVerfier {
    private final AccountTokensRepository accountTokensRepository;

    @Inject
    public AuthorizationCodeVerifier(final AccountTokensRepository accountTokensRepository) {
        this.accountTokensRepository = accountTokensRepository;
    }

    @Override
    public Optional<String> verifyAccountToken(final String token) {
        return verifyAndGetAccountToken(token)
                .map(AccountTokenDO::getAssociatedAccountId);
    }

    @Override
    public Optional<AccountTokenDO> verifyAndGetAccountToken(final String token) {
        final AccountTokenDO accountToken = accountTokensRepository.getByToken(token)
                .join()
                .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid authorization code " + token));

        if (accountToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "The authorization code has expired");
        }

        return Optional.of(accountToken);
    }
}