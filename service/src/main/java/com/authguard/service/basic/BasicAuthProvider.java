package com.authguard.service.basic;

import com.authguard.service.AccountsService;
import com.authguard.service.CredentialsService;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.helpers.TokensUtils;
import com.authguard.service.passwords.SecurePassword;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.CredentialsBO;
import com.google.inject.Inject;

import java.util.Base64;
import java.util.Optional;

public class BasicAuthProvider {
    private final CredentialsService credentialsService;
    private final AccountsService accountsService;
    private final SecurePassword securePassword;

    @Inject
    public BasicAuthProvider(final CredentialsService credentialsService, final AccountsService accountsService,
                             final SecurePassword securePassword) {
        this.credentialsService = credentialsService;
        this.securePassword = securePassword;
        this.accountsService = accountsService;
    }

    public Optional<AccountBO> authenticateAndGetAccount(final String basicToken) {
        final String[] parts = TokensUtils.parseAuthorization(basicToken);

        if (parts[0].equals("Basic")) {
            return handleBasicAuthentication(parts[1]);
        } else {
            throw new ServiceException(ErrorCode.UNSUPPORTED_SCHEME, "Unsupported authorization scheme");
        }
    }

    public Optional<AccountBO> getAccount(final String basicToken) {
        final String[] parts = TokensUtils.parseAuthorization(basicToken);

        if (parts[0].equals("Basic")) {
            return handleBasicAuthenticationNoPassword(parts[1]);
        } else {
            throw new ServiceException(ErrorCode.UNSUPPORTED_SCHEME, "Unsupported authorization scheme");
        }
    }

    private Optional<AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        return verifyCredentials(username, password);
    }

    private Optional<AccountBO> handleBasicAuthenticationNoPassword(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 1) {
            throw new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for basic authentication");
        }

        final String username =  decoded[0];

        return verifyCredentials(username);
    }

    private Optional<AccountBO> verifyCredentials(final String username, final String password) {
        final Optional<CredentialsBO> credentials = credentialsService.getByUsernameUnsafe(username);

        if (credentials.isPresent()) {
            if (securePassword.verify(password, credentials.get().getHashedPassword())) {
                return accountsService.getById(credentials.get().getAccountId());
            } else {
                throw new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords don't match");
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<AccountBO> verifyCredentials(final String username) {
        return credentialsService.getByUsernameUnsafe(username)
                .map(CredentialsBO::getAccountId)
                .flatMap(accountsService::getById);
    }

}
