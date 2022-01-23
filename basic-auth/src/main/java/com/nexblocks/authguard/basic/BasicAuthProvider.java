package com.nexblocks.authguard.basic;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.CredentialsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

public class BasicAuthProvider {
    private static final String RESERVED_DOMAIN = "global";

    private final Logger LOG = LoggerFactory.getLogger(BasicAuthProvider.class);

    private final CredentialsService credentialsService;
    private final AccountsService accountsService;
    private final SecurePassword securePassword;
    private final SecurePasswordProvider securePasswordProvider;

    @Inject
    public BasicAuthProvider(final CredentialsService credentialsService, final AccountsService accountsService,
                             final SecurePasswordProvider securePasswordProvider) {
        this.credentialsService = credentialsService;
        this.securePassword = securePasswordProvider.get();
        this.accountsService = accountsService;
        this.securePasswordProvider = securePasswordProvider;

        LOG.debug("Initialized with password implementation {}", this.securePassword.getClass());
    }

    public Either<Exception, AccountBO> authenticateAndGetAccount(final AuthRequestBO authRequest) {
        return verifyCredentialsAndGetAccount(authRequest.getIdentifier(), authRequest.getPassword(), authRequest.getDomain());
    }

    public Either<Exception, AccountBO> authenticateAndGetAccount(final String basicToken) {
        return handleBasicAuthentication(basicToken);
    }

    public Either<Exception, AccountBO> getAccount(final AuthRequestBO request) {
        return verifyCredentialsAndGetAccount(request.getIdentifier(), request.getDomain());
    }

    private Either<Exception, AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        return verifyCredentialsAndGetAccount(username, password, RESERVED_DOMAIN);
    }

    private Either<Exception, AccountBO> verifyCredentialsAndGetAccount(final String username, final String password, final String domain) {
        final Optional<CredentialsBO> credentialsOpt = credentialsService.getByUsernameUnsafe(username, domain);

        // TODO replace this with Either mapping
        if (credentialsOpt.isPresent()) {
            final CredentialsBO credentials = credentialsOpt.get();
            final Optional<Exception> validationError = checkIdentifier(credentials, username);

            if (validationError.isPresent()) {
                return Either.left(validationError.get());
            }

            return checkIfExpired(credentials)
                    .flatMap(valid -> checkPasswordsMatch(valid, password))
                    .flatMap(valid -> getAccountById(valid.getAccountId()));
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                    "Identifier " + username + " does not exist"));
        }
    }

    private Either<Exception, CredentialsBO> checkIfExpired(final CredentialsBO credentials) {
        // check if expired
        if (securePasswordProvider.passwordsExpire()) {
            if (credentials.getPasswordUpdatedAt() == null) {
                return Either.left(new IllegalStateException("Credentials " + credentials.getId() + " passwordUpdatedAt was null"));
            }

            final OffsetDateTime expiresAt = credentials.getPasswordUpdatedAt()
                    .plus(securePasswordProvider.getPasswordTtl());

            if (expiresAt.isBefore(OffsetDateTime.now())) {
                return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORD_EXPIRED,
                        "Password has already expired", EntityType.ACCOUNT, credentials.getAccountId()));
            }
        }

        // check if the version is too old
        if (credentials.getPasswordVersion() < securePasswordProvider.getMinimumVersion()) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORD_EXPIRED,
                    "Password has already expired", EntityType.ACCOUNT, credentials.getAccountId()));
        }

        return Either.right(credentials);
    }

    private Either<Exception, CredentialsBO> checkPasswordsMatch(final CredentialsBO credentials, final String password) {
        final SecurePassword securePasswordImplementation = getPasswordImplementation(credentials);

        if (securePasswordImplementation == null) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Unable to map password version", EntityType.ACCOUNT, credentials.getAccountId()));
        }

        if (securePasswordImplementation.verify(password, credentials.getHashedPassword())) {
            return Either.right(credentials);
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH,
                    "Passwords do not match", EntityType.ACCOUNT, credentials.getAccountId()));
        }
    }

    private SecurePassword getPasswordImplementation(final CredentialsBO credentials) {
        if (credentials.getPasswordVersion() == null
                || credentials.getPasswordVersion().equals(securePasswordProvider.getCurrentVersion())) {
            return securePassword;
        }

        return securePasswordProvider.getPreviousVersions().get(credentials.getPasswordVersion());
    }

    private Either<Exception, AccountBO> verifyCredentialsAndGetAccount(final String username, final String domain) {
        final Optional<CredentialsBO> credentials = credentialsService.getByUsernameUnsafe(username, domain);

        if (credentials.isPresent()) {
            final Optional<Exception> validationError = checkIdentifier(credentials.get(), username);

            if (validationError.isPresent()) {
                return Either.left(validationError.get());
            }

            return getAccountById(credentials.get().getAccountId());
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                    "Identifier " + username + " does not exist"));
        }
    }

    private Optional<Exception> checkIdentifier(final CredentialsBO credentials,
                                                final String identifier) {
        final Optional<UserIdentifierBO> matchedIdentifier = credentials.getIdentifiers()
                .stream()
                .filter(existing -> identifier.equals(existing.getIdentifier()))
                .findFirst();

        if (matchedIdentifier.isEmpty()) {
            return Optional.of(new IllegalStateException("No identifier matched but credentials were returned"));
        }

        if (!matchedIdentifier.get().isActive()) {
            return Optional.of(new ServiceAuthorizationException(ErrorCode.INACTIVE_IDENTIFIER,
                    "Identifier is not active", EntityType.ACCOUNT, credentials.getAccountId()));
        }

        return Optional.empty();
    }

    private Either<Exception, AccountBO> getAccountById(final String accountId) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(account -> {
                    if (account.isActive()) {
                        return Either.right(account);
                    }

                    return Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE,
                            "Account " + accountId + " was deactivated"));
                })
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Account " + accountId + " does not exist")));
    }
}
