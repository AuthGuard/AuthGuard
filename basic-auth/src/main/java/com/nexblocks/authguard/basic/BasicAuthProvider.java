package com.nexblocks.authguard.basic;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public class BasicAuthProvider {
    private static final String RESERVED_DOMAIN = "global";

    private final Logger LOG = LoggerFactory.getLogger(BasicAuthProvider.class);

    private final AccountsService accountsService;
    private final SecurePassword securePassword;
    private final SecurePasswordProvider securePasswordProvider;

    @Inject
    public BasicAuthProvider(final AccountsService accountsService,
                             final SecurePasswordProvider securePasswordProvider) {
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
        final Optional<AccountBO> credentialsOpt = accountsService.getByIdentifierUnsafe(username, domain);

        // TODO replace this with Either mapping
        return credentialsOpt
                .map(account -> tryVerifyCredentials(account, username, password))
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                        "Identifier " + username + " does not exist")));
    }

    private Either<Exception, AccountBO> tryVerifyCredentials(final AccountBO account, final String identifier,
                                                              final String password) {
        final Optional<Exception> validationError = checkIdentifier(account, identifier);

        if (validationError.isPresent()) {
            return Either.left(validationError.get());
        }

        return checkIfExpired(account)
                .flatMap(valid -> checkPasswordsMatch(valid, password));
    }

    private Either<Exception, AccountBO> checkIfExpired(final AccountBO credentials) {
        // check if expired
        if (securePasswordProvider.passwordsExpire()) {
            if (credentials.getPasswordUpdatedAt() == null) {
                return Either.left(new IllegalStateException("Credentials " + credentials.getId() + " passwordUpdatedAt was null"));
            }

            final Instant expiresAt = credentials.getPasswordUpdatedAt()
                    .plus(securePasswordProvider.getPasswordTtl());

            if (expiresAt.isBefore(Instant.now())) {
                return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORD_EXPIRED,
                        "Password has already expired", EntityType.ACCOUNT, credentials.getId()));
            }
        }

        // check if the version is too old
        if (credentials.getPasswordVersion() < securePasswordProvider.getMinimumVersion()) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORD_EXPIRED,
                    "Password has already expired", EntityType.ACCOUNT, credentials.getId()));
        }

        return Either.right(credentials);
    }

    private Either<Exception, AccountBO> checkPasswordsMatch(final AccountBO credentials, final String password) {
        final SecurePassword securePasswordImplementation = getPasswordImplementation(credentials);

        if (securePasswordImplementation == null) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Unable to map password version", EntityType.ACCOUNT, credentials.getId()));
        }

        if (securePasswordImplementation.verify(password, credentials.getHashedPassword())) {
            return Either.right(credentials);
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH,
                    "Passwords do not match", EntityType.ACCOUNT, credentials.getId()));
        }
    }

    private SecurePassword getPasswordImplementation(final AccountBO credentials) {
        if (credentials.getPasswordVersion() == null
                || credentials.getPasswordVersion().equals(securePasswordProvider.getCurrentVersion())) {
            return securePassword;
        }

        return securePasswordProvider.getPreviousVersions().get(credentials.getPasswordVersion());
    }

    private Either<Exception, AccountBO> verifyCredentialsAndGetAccount(final String username, final String domain) {
        final Optional<AccountBO> credentials = accountsService.getByIdentifierUnsafe(username, domain);

        if (credentials.isPresent()) {
            final Optional<Exception> validationError = checkIdentifier(credentials.get(), username);

            if (validationError.isPresent()) {
                return Either.left(validationError.get());
            }

            return Either.right(credentials.get());
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                    "Identifier " + username + " does not exist"));
        }
    }

    private Optional<Exception> checkIdentifier(final AccountBO credentials,
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
                    "Identifier is not active", EntityType.ACCOUNT, credentials.getId()));
        }

        return Optional.empty();
    }
}
