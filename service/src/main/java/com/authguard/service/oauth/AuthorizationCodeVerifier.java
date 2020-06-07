package com.authguard.service.oauth;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AuthTokenVerfier;
import com.authguard.service.exceptions.ServiceAuthorizationException;
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
                .orElseThrow(() -> new ServiceAuthorizationException("Invalid authorization code " + token));

        if (accountToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException("The authorization code has expired");
        }

        return Optional.of(accountToken);
    }
}
