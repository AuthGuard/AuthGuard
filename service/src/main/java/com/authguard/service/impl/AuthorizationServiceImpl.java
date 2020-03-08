package com.authguard.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.authguard.service.AccountsService;
import com.authguard.service.AuthorizationService;
import com.authguard.service.JwtProvider;
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
    private final JwtProvider idTokenProvider;
    private final JwtProvider accessTokenProvider;
    private final AccountTokensRepository accountTokensRepository;
    private final ImmutableStrategyConfig accessTokenStrategy;

    @Inject
    public AuthorizationServiceImpl(final AccountsService accountsService,
                                    @Named("authenticationTokenProvider") final JwtProvider idTokenProvider,
                                    @Named("authorizationTokenProvider") final JwtProvider accessTokenProvider,
                                    final AccountTokensRepository accountTokensRepository,
                                    @Named("accessToken") final ImmutableStrategyConfig accessTokenStrategy) {
        this.accountsService = accountsService;
        this.idTokenProvider = idTokenProvider;
        this.accessTokenProvider = accessTokenProvider;
        this.accountTokensRepository = accountTokensRepository;
        this.accessTokenStrategy = accessTokenStrategy;
    }

    @Override
    public Optional<TokensBO> authorize(final String header) {
        final String[] parts = parseAuthorization(header);

        if (parts[0].equals("Bearer")) {
            return handleIdTokenAuthorization(parts[1]);
        } else {
            throw new ServiceException("Unsupported authorization scheme");
        }
    }

    @Override
    public Optional<TokensBO> refresh(final String refreshToken) {
        final Optional<AccountTokenDO> accountToken = accountTokensRepository.getByToken(refreshToken);

        if (accountToken.isEmpty()) {
            throw new ServiceAuthorizationException("Non-existing or expired service token");
        }

        return accountToken
                .filter(this::validateExpirationDateTime)
                .map(AccountTokenDO::getAssociatedAccountId)
                .flatMap(this::generateTokenForAccount);
    }

    private Optional<TokensBO> handleIdTokenAuthorization(final String token) {
        final String associatedAccountId = getAssociatedAccountId(validateToken(token));

        return generateTokenForAccount(associatedAccountId);
    }

    private Optional<TokensBO> generateTokenForAccount(final String accountId) {
        final Optional<TokensBO> tokens = Optional.of(accountId)
                .map(this::getAccount)
                .map(accessTokenProvider::generateToken);

        tokens.map(TokensBO::getRefreshToken)
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

    private DecodedJWT validateToken(final String token) {
        return idTokenProvider.validateToken(token)
                .orElseThrow(() -> new ServiceAuthorizationException("Failed to authenticate token " + token));
    }

    private String getAssociatedAccountId(final DecodedJWT token) {
        return token.getSubject();
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
