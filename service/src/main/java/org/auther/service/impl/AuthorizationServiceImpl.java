package org.auther.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Inject;
import com.google.inject.name.Named;
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
    private final JwtProvider idTokenProvider;
    private final JwtProvider accessTokenProvider;

    @Inject
    public AuthorizationServiceImpl(final AccountsService accountsService,
                                    @Named("authenticationTokenProvider") final JwtProvider idTokenProvider,
                                    @Named("authorizationTokenProvider") final JwtProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.idTokenProvider = idTokenProvider;
        this.accessTokenProvider = accessTokenProvider;
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

    private Optional<TokensBO> handleIdTokenAuthorization(final String token) {
        return Optional.of(validate(token))
                .map(this::getAssociatedAccountId)
                .map(this::getAccount)
                .map(accessTokenProvider::generateToken);
    }

    private DecodedJWT validate(final String token) {
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
}
