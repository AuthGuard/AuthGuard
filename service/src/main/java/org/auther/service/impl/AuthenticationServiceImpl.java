package org.auther.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.auther.service.*;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.CredentialsBO;
import org.auther.service.model.TokensBO;

import java.util.Base64;
import java.util.Optional;

public class AuthenticationServiceImpl implements AuthenticationService {
    private final CredentialsService credentialsService;
    private final AccountsService accountsService;
    private final SecurePassword securePassword;
    private final JwtProvider jwtProvider;

    @Inject
    public AuthenticationServiceImpl(final CredentialsService credentialsService, final AccountsService accountsService,
                                     final SecurePassword securePassword,
                                     @Named("authenticationTokenProvider") final JwtProvider jwtProvider) {
        this.credentialsService = credentialsService;
        this.accountsService = accountsService;
        this.securePassword = securePassword;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Optional<TokensBO> authenticate(final String header) {
        final String[] parts = parseAuthorization(header);

        if (parts[0].equals("Basic")) {
            return handleBasicAuthentication(parts[1])
                    .map(jwtProvider::generateToken);
        } else {
            throw new ServiceException("Unsupported authorization scheme");
        }
    }

    private Optional<AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException("Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        final Optional<CredentialsBO> credentials = credentialsService.getByUsername(username);

        if (credentials.isPresent()) {
            if (securePassword.verify(password, credentials.get().getHashedPassword())) {
                return accountsService.getById(credentials.get().getAccountId());
            } else {
                throw new ServiceAuthorizationException("Passwords don't match");
            }
        } else {
            return Optional.empty();
        }
    }

    private String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException("Invalid format for authorization value");
        }

        return parts;
    }
}
