package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.*;
import com.authguard.service.exceptions.ServiceException;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.authguard.service.*;
import com.authguard.service.config.ImmutableAuthenticationConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.CredentialsBO;
import com.authguard.service.model.TokensBO;

import java.util.Base64;
import java.util.Optional;

public class AuthenticationServiceImpl implements AuthenticationService {
    private final CredentialsService credentialsService;
    private final OtpService otpService;
    private final AccountsService accountsService;
    private final SecurePassword securePassword;
    private final JwtProvider jwtProvider;
    private final ImmutableAuthenticationConfig authenticationConfig;

    @Inject
    public AuthenticationServiceImpl(final CredentialsService credentialsService,
                                     final OtpService otpService,
                                     final AccountsService accountsService,
                                     final SecurePassword securePassword,
                                     @Named("authenticationTokenProvider") final JwtProvider jwtProvider,
                                     @Named("authentication") final ConfigContext authenticationConfig) {
        this.credentialsService = credentialsService;
        this.otpService = otpService;
        this.accountsService = accountsService;
        this.securePassword = securePassword;
        this.jwtProvider = jwtProvider;
        this.authenticationConfig = authenticationConfig.asConfigBean(ImmutableAuthenticationConfig.class);
    }

    @Override
    public Optional<TokensBO> authenticate(final String header) {
        final String[] parts = parseAuthorization(header);

        if (parts[0].equals("Basic")) {
            final Optional<AccountBO> account =  handleBasicAuthentication(parts[1]);

            if (authenticationConfig.getUseOtp()) {
                return account.map(otpService::generate);
            } else {
                return account.map(jwtProvider::generateToken);
            }
        } else {
            throw new ServiceException("Unsupported authorization scheme");
        }
    }

    @Override
    public Optional<AccountBO> authenticate(final String username, final String password) {
        return verifyCredentials(username, password);
    }

    private Optional<AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException("Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        return verifyCredentials(username, password);
    }

    private Optional<AccountBO> verifyCredentials(final String username, final String password) {
        final Optional<CredentialsBO> credentials = credentialsService.getByUsernameUnsafe(username);

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
