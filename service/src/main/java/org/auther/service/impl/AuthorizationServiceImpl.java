package org.auther.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.auther.dal.AccountTokensRepository;
import org.auther.dal.model.AccountTokenDO;
import org.auther.service.AccountsService;
import org.auther.service.AuthorizationService;
import org.auther.service.JwtProvider;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public class AuthorizationServiceImpl implements AuthorizationService {
    private final AccountsService accountsService;
    private final AccountTokensRepository accountTokensRepository;
    private final JwtProvider idTokenProvider;
    private final JwtProvider accessTokenProvider;

    @Inject
    public AuthorizationServiceImpl(final AccountsService accountsService,
                                    final AccountTokensRepository accountTokensRepository,
                                    @Named("authenticationTokenProvider") final JwtProvider idTokenProvider,
                                    @Named("authorizationTokenProvider") final JwtProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.accountTokensRepository = accountTokensRepository;
        this.idTokenProvider = idTokenProvider;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Optional<TokensBO> authorize(final String header) {
        final String[] parts = parseAuthorization(header);

        if (parts[0].equals("Bearer")) {
            return Optional.ofNullable(handleIdTokenAuthorization(parts[1]));
        } else {
            throw new ServiceException("Unsupported authorization scheme");
        }
    }

    private TokensBO handleIdTokenAuthorization(final String token) {
        return Optional.of(validate(token))
                .map(this::getAssociatedAccountId)
                .map(this::getAccount)
                .map(accessTokenProvider::generateToken)
                .orElseThrow(() -> new IllegalStateException("Unexpected error while authorizing token"));
    }

    private String validate(final String token) {
        final Optional<DecodedJWT> decodedToken = idTokenProvider.validateToken(token);

        if (decodedToken.isEmpty()) {
            throw new ServiceAuthorizationException("Failed to authenticate token " + token);
        }

        return token;
    }

    private String getAssociatedAccountId(final String token) {
        final Optional<AccountTokenDO> accountToken = accountTokensRepository.getByToken(token);

        if (accountToken.isEmpty()) {
            throw new ServiceAuthorizationException("Token " + token + " has no associated account");
        }

        return accountToken.get().getAssociatedAccountId();
    }

    private AccountBO getAccount(final String accountId) {
        final Optional<AccountBO> account = accountsService.getById(accountId);

        // the account could have been deleted or deactivated
        if (account.isEmpty()) {
            throw new ServiceAuthorizationException("Could not find account " + accountId);
        }

        return account.get();
    }

    private String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException("Invalid format for authorization value");
        }

        return parts;
    }
}
