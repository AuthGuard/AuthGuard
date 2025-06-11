package com.nexblocks.authguard.service.util;

import com.nexblocks.authguard.basic.passwords.*;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.HashedPasswordBO;
import com.nexblocks.authguard.service.model.UserIdentifier;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import com.google.inject.Inject;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CredentialsManager {
    private final SecurePassword securePassword;
    private final PasswordValidator passwordValidator;
    private final Integer passwordVersion;

    @Inject
    public CredentialsManager(final SecurePasswordProvider securePasswordProvider,
                              final PasswordValidator passwordValidator) {
        this.securePassword = securePasswordProvider.get();
        this.passwordValidator = passwordValidator;
        this.passwordVersion = securePasswordProvider.getCurrentVersion();
    }

    public AccountBO removeSensitiveInformation(final AccountBO account) {
        return account.withPlainPassword(null)
                .withHashedPassword(null);
    }

    public Uni<AccountBO> verifyAndHashPlainPassword(final AccountBO account) {
        return verifyAndHashPassword(account.getPlainPassword())
                .map(hashedPassword -> AccountBO.builder()
                        .from(account)
                        .hashedPassword(hashedPassword)
                        .passwordUpdatedAt(Instant.now())
                        .passwordVersion(passwordVersion)
                        .plainPassword(null)
                        .build());
    }

    public AccountBO addOrReplaceIdentifier(final AccountBO account, final String oldValue, final String newValue,
                                            final UserIdentifier.Type type) {
        if (!hasIdentifier(account, oldValue)) {
            return addIdentifiers(account, UserIdentifierBO.builder()
                    .identifier(newValue)
                    .type(type)
                    .build());
        }

        return replaceIdentifier(account, oldValue, UserIdentifierBO.builder()
                .identifier(newValue)
                .type(type)
                .build());
    }

    public Uni<HashedPasswordBO> verifyAndHashPassword(final String plain) {
        final List<Violation> passwordViolations = passwordValidator.findViolations(plain);

        if (!passwordViolations.isEmpty()) {
            throw new ServiceInvalidPasswordException(passwordViolations);
        }

        return securePassword.hash(plain);
    }

    public AccountBO addIdentifiers(final AccountBO existing, final UserIdentifierBO identifier) {
        final Set<String> existingIdentifiers = existing.getIdentifiers().stream()
                .map(UserIdentifierBO::getIdentifier)
                .collect(Collectors.toSet());

        final List<UserIdentifierBO> combined = new ArrayList<>(existing.getIdentifiers());

        if (existingIdentifiers.contains(identifier.getIdentifier())) {
            throw new ServiceConflictException(ErrorCode.IDENTIFIER_ALREADY_EXISTS,
                    "Identifier already exists");
        }

        combined.add(UserIdentifierBO.builder().from(identifier)
                .active(true)
                .domain(existing.getDomain())
                .build());

        return AccountBO.builder().from(existing)
                .identifiers(combined)
                .build();
    }

    public AccountBO replaceIdentifier(final AccountBO existing, final String oldIdentifier,
                                       final UserIdentifierBO newIdentifier) {
        final Set<UserIdentifierBO> newIdentifiers = existing.getIdentifiers()
                .stream()
                .map(identifier -> {
                    if (identifier.getIdentifier().equals(oldIdentifier)) {
                        return UserIdentifierBO.builder()
                                .identifier(newIdentifier.getIdentifier())
                                .active(true) // TODO should it always be active? maybe it needs to be verified first
                                .type(identifier.getType())
                                .domain(identifier.getDomain())
                                .build();
                    }

                    return identifier;
                })
                .collect(Collectors.toSet());

        return existing.withIdentifiers(newIdentifiers);
    }

    private boolean hasIdentifier(final AccountBO account, final String identifier) {
        return account.getIdentifiers()
                .stream()
                .anyMatch(userIdentifier -> userIdentifier.getIdentifier().equals(identifier));
    }
}
