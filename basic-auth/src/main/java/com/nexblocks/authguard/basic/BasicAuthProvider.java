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
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<AccountBO> authenticateAndGetAccount(final AuthRequestBO authRequest) {
        return verifyCredentialsAndGetAccount(authRequest.getIdentifier(), authRequest.getPassword(), authRequest.getDomain());
    }

    /**
     * Performs basic authentication using a basic token in the form of
     * base64(username:password). To be used only for authenticating request
     * 'Authorization' headers made to AuthGuard. For any other scenario, use
     * authenticateAndGetAccount(AuthRequest);
     */
    public CompletableFuture<AccountBO> authenticateAndGetAccount(final String basicToken) {
        return handleBasicAuthentication(basicToken);
    }

    public AccountBO getAccount(final AuthRequestBO request) {
        return verifyCredentialsAndGetAccount(request.getIdentifier(), request.getDomain()).join();
    }

    public CompletableFuture<AccountBO> getAccountAsync(final AuthRequestBO request) {
        return verifyCredentialsAndGetAccount(request.getIdentifier(), request.getDomain());
    }

    private CompletableFuture<AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        return verifyCredentialsAndGetAccount(username, password, RESERVED_DOMAIN);
    }

    private CompletableFuture<AccountBO> verifyCredentialsAndGetAccount(final String username, final String password, final String domain) {
        return accountsService.getByIdentifierUnsafe(username, domain)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                                "Identifier does not exist"));
                    }

                    return AsyncUtils.fromTry(tryVerifyCredentials(opt.get(), username, password));
                });
    }

    private Try<AccountBO> tryVerifyCredentials(final AccountBO account, final String identifier, final String password) {
        if (!account.isActive()) {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE, "Inactive account"));
        }

        final Optional<Exception> validationError = checkIdentifier(account, identifier);

        if (validationError.isPresent()) {
            return Try.failure(validationError.get());
        }

        return checkIfExpired(account)
                .flatMap(valid -> checkPasswordsMatch(valid, password));
    }

    private Try<AccountBO> checkIfExpired(final AccountBO credentials) {
        // check if expired
        if (securePasswordProvider.passwordsExpire()) {
            if (credentials.getPasswordUpdatedAt() == null) {
                return Try.failure(new IllegalStateException("Credentials " + credentials.getId() + " passwordUpdatedAt was null"));
            }

            final Instant expiresAt = credentials.getPasswordUpdatedAt()
                    .plus(securePasswordProvider.getPasswordTtl());

            if (expiresAt.isBefore(Instant.now())) {
                return Try.failure(new ServiceAuthorizationException(ErrorCode.PASSWORD_EXPIRED,
                        "Password has already expired", EntityType.ACCOUNT, credentials.getId()));
            }
        }

        // check if the version is too old
        if (credentials.getPasswordVersion() < securePasswordProvider.getMinimumVersion()) {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.PASSWORD_EXPIRED,
                    "Password has already expired", EntityType.ACCOUNT, credentials.getId()));
        }

        return Try.success(credentials);
    }

    private Try<AccountBO> checkPasswordsMatch(final AccountBO credentials, final String password) {
        final SecurePassword securePasswordImplementation = getPasswordImplementation(credentials);

        if (securePasswordImplementation == null) {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Unable to map password version", EntityType.ACCOUNT, credentials.getId()));
        }

        if (securePasswordImplementation.verify(password, credentials.getHashedPassword())) {
            return Try.success(credentials);
        } else {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH,
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

    private CompletableFuture<AccountBO> verifyCredentialsAndGetAccount(final String username, final String domain) {
        return accountsService.getByIdentifierUnsafe(username, domain)
                .thenCompose(credentials -> {
                    if (credentials.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                                "Identifier does not exist"));
                    }

                    Optional<Exception> validationError = checkIdentifier(credentials.get(), username);

                    if (validationError.isPresent()) {
                        return CompletableFuture.failedFuture(validationError.get());
                    }

                    return CompletableFuture.completedFuture(credentials.get());
                });
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
