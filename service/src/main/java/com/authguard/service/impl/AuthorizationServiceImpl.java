package com.authguard.service.impl;

import com.authguard.service.AccountsService;
import com.authguard.service.AuthTokenVerfier;
import com.authguard.service.AuthorizationService;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.exceptions.ServiceException;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;

import java.time.ZonedDateTime;
import java.util.Optional;

public class AuthorizationServiceImpl implements AuthorizationService {
    private final AccountsService accountsService;
    private final AuthTokenVerfier authenticationTokenVerifier;
    private final AuthProvider accessTokenProvider;
    private final AccountTokensRepository accountTokensRepository;
    private final ImmutableStrategyConfig accessTokenStrategy;

    @Inject
    public AuthorizationServiceImpl(final AccountsService accountsService,
                                    @Named("authenticationTokenVerifier") AuthTokenVerfier authenticationTokenVerifier,
                                    @Named("authorizationTokenProvider") final AuthProvider authorizationProvider,
                                    final AccountTokensRepository accountTokensRepository,
                                    @Named("accessToken") final ImmutableStrategyConfig accessTokenStrategy) {
        this.accountsService = accountsService;
        this.authenticationTokenVerifier = authenticationTokenVerifier;
        this.accessTokenProvider = authorizationProvider;
        this.accountTokensRepository = accountTokensRepository;
        this.accessTokenStrategy = accessTokenStrategy;
    }

    @Override
    public TokensBO authorize(final String header) {
        final String[] parts = parseAuthorization(header);

        if (parts[0].equals("Bearer")) {
            return handleIdTokenAuthorization(parts[1]);
        } else {
            throw new ServiceException("Unsupported authorization scheme");
        }
    }

    @Override
    public TokensBO refresh(final String refreshToken) {
        return accountTokensRepository.getByToken(refreshToken)
                .thenApply(optional -> optional
                        .filter(this::validateExpirationDateTime)
                        .map(AccountTokenDO::getAssociatedAccountId)
                        .map(this::generateTokenForAccount)
                        .orElseThrow(() -> new ServiceAuthorizationException("Non-existing or expired refresh token")))
                .join();
    }

    private TokensBO handleIdTokenAuthorization(final String token) {
        final String associatedAccountId = validateToken(token);

        return generateTokenForAccount(associatedAccountId);
    }

    private TokensBO generateTokenForAccount(final String accountId) {
        final TokensBO tokens = Optional.of(accountId)
                .map(this::getAccount)
                .map(accessTokenProvider::generateToken)
                .orElseThrow(IllegalStateException::new);

        Optional.ofNullable(tokens.getRefreshToken())
                .map(refreshToken -> AccountTokenDO.builder()
                        .token(refreshToken)
                        .associatedAccountId(accountId)
                        .expiresAt(getExpirationDateTime())
                        .build()
                )
                .ifPresentOrElse(accountTokensRepository::save, () -> {
                    throw new ServiceAuthorizationException("Failed to associate account " + accountId
                            + " with a refresh token");
                });

        return tokens;
    }

    private String validateToken(final String token) {
        return authenticationTokenVerifier.verifyAccountToken(token)
                .orElseThrow(() -> new ServiceAuthorizationException("Failed to authenticate token " + token));
    }

    private AccountBO getAccount(final String accountId) {
        return accountsService.getById(accountId)
                .orElseThrow(() -> new ServiceAuthorizationException("Could not find account " + accountId));
    }

    private String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException("Invalid format for authorization value");
        }

        return parts;
    }

    private ZonedDateTime getExpirationDateTime() {
        return ZonedDateTime.now()
                .plus(ConfigParser.parseDuration(accessTokenStrategy.getRefreshTokenLife()));
    }

    private boolean validateExpirationDateTime(final AccountTokenDO accountToken) {
        final ZonedDateTime now = ZonedDateTime.now();

        if (now.isAfter(accountToken.expiresAt())) {
            throw new ServiceAuthorizationException("Refresh token " + accountToken.expiresAt()
                    + " for account " + accountToken.getAssociatedAccountId() + " has expired");
        }

        return true;
    }
}
