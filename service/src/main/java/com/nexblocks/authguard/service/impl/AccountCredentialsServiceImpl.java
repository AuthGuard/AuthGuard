package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.persistence.CredentialsAuditRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.AccountCredentialsService;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.messaging.ResetTokenMessage;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.CredentialsManager;
import com.nexblocks.authguard.service.util.ID;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AccountCredentialsServiceImpl implements AccountCredentialsService {
    private static final String CREDENTIALS_CHANNEL = "credentials";
    private static final int RESET_TOKEN_SIZE = 128;
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final AccountsService accountsService;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final AccountTokensRepository accountTokensRepository;
    private final SecurePassword securePassword;
    private final CredentialsManager credentialsManager;
    private final MessageBus messageBus;
    private final ServiceMapper serviceMapper;

    private final CryptographicRandom cryptographicRandom;

    @Inject
    public AccountCredentialsServiceImpl(final AccountsService accountsService,
                                         final CredentialsAuditRepository credentialsAuditRepository,
                                         final AccountTokensRepository accountTokensRepository,
                                         final SecurePasswordProvider securePasswordProvider,
                                         final CredentialsManager credentialsManager,
                                         final MessageBus messageBus,
                                         final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.credentialsManager = credentialsManager;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.accountTokensRepository = accountTokensRepository;
        this.securePassword = securePasswordProvider.get();
        this.messageBus = messageBus;
        this.serviceMapper = serviceMapper;

        this.cryptographicRandom = new CryptographicRandom();
    }

    public Optional<AccountBO> getByIdUnsafe(final String id) {
        return accountsService.getByIdUnsafe(id);
    }

    @Override
    public Optional<AccountBO> updatePassword(final String id, final String plainPassword) {
        final AccountBO existing = accountsService.getById(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final HashedPasswordBO newPassword = credentialsManager.verifyAndHashPassword(plainPassword);
        final AccountBO update = existing
                .withHashedPassword(newPassword)
                .withPasswordUpdatedAt(Instant.now());

        return doUpdate(existing, update)
                .map(result -> {
                    storePasswordUpdateRecord(existing);

                    return result;
                });
    }

    @Override
    public Optional<AccountBO> addIdentifiers(final String id, final List<UserIdentifierBO> identifiers) {
        final AccountBO existing = getByIdUnsafe(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final Set<String> existingIdentifiers = existing.getIdentifiers().stream()
                .map(UserIdentifierBO::getIdentifier)
                .collect(Collectors.toSet());

        final List<UserIdentifierBO> combined = new ArrayList<>(existing.getIdentifiers());

        for (final UserIdentifierBO identifier : identifiers) {
            if (existingIdentifiers.contains(identifier.getIdentifier())) {
                throw new ServiceConflictException(ErrorCode.IDENTIFIER_ALREADY_EXISTS, "Duplicate identifier for " + id);
            }

            combined.add(UserIdentifierBO.builder().from(identifier)
                    .active(true)
                    .domain(existing.getDomain())
                    .build());
        }

        final AccountBO updated = AccountBO.builder().from(existing)
                .identifiers(combined)
                .build();

        return doUpdate(existing, updated);
    }

    @Override
    public Optional<AccountBO> removeIdentifiers(final String id, final List<String> identifiers) {
        final AccountBO existing = getByIdUnsafe(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final List<UserIdentifierBO> updatedIdentifiers = existing.getIdentifiers().stream()
                .map(identifier -> {
                    if (identifiers.contains(identifier.getIdentifier())) {
                        return identifier.withActive(false);
                    }

                    return identifier;
                })
                .collect(Collectors.toList());

        final AccountBO updated = AccountBO.builder().from(existing)
                .identifiers(updatedIdentifiers)
                .build();

        return doUpdate(existing, updated)
                .map(result -> {
                    updatedIdentifiers.forEach(oldIdentifier ->
                            storeIdentifierUpdateRecord(existing, oldIdentifier, CredentialsAudit.Action.DEACTIVATED));

                    return result;
                });
    }

    @Override
    public Optional<AccountBO> replaceIdentifier(final String id, final String oldIdentifier, final UserIdentifierBO newIdentifier) {
        final AccountBO credentials = getByIdUnsafe(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No account with ID " + id));

        final Optional<UserIdentifierBO> matchedIdentifier = credentials.getIdentifiers()
                .stream()
                .filter(identifier -> identifier.getIdentifier().equals(oldIdentifier))
                .findFirst();

        if (matchedIdentifier.isEmpty()) {
            throw new ServiceException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "Credentials " + id + " has no identifier " + oldIdentifier);
        }

        final Set<UserIdentifierBO> newIdentifiers = credentials.getIdentifiers()
                .stream()
                .map(identifier -> {
                    if (identifier.getIdentifier().equals(oldIdentifier)) {
                        return UserIdentifierBO.builder()
                                .identifier(newIdentifier.getIdentifier())
                                .active(newIdentifier.isActive())
                                .type(identifier.getType())
                                .domain(identifier.getDomain())
                                .build();
                    }

                    return identifier;
                })
                .collect(Collectors.toSet());

        final AccountBO update = credentials.withIdentifiers(newIdentifiers);

        return doUpdate(credentials, update)
                .map(result -> {
                    storeIdentifierUpdateRecord(credentials, matchedIdentifier.get(), CredentialsAudit.Action.UPDATED);

                    return result;
                });
    }

    @Override
    public PasswordResetTokenBO generateResetToken(final String identifier, final boolean returnToken, final String domain) {
        final AccountBO account = accountsService.getByIdentifier(identifier, domain)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "Unknown identifier"));

        final Instant now = Instant.now();

        final AccountTokenDO accountToken = AccountTokenDO
                .builder()
                .id(ID.generate())
                .token(cryptographicRandom.base64Url(RESET_TOKEN_SIZE))
                .associatedAccountId(account.getId())
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .build();

        final AccountTokenDO persistedToken = accountTokensRepository.save(accountToken).join();

        messageBus.publish(CREDENTIALS_CHANNEL,
                Messages.resetTokenGenerated(new ResetTokenMessage(account, persistedToken)));

        return PasswordResetTokenBO.builder()
                .token(returnToken ? persistedToken.getToken() : null)
                .issuedAt(now.toEpochMilli() / 1000)
                .expiresAt(persistedToken.getExpiresAt().toEpochMilli() / 1000)
                .build();
    }

    @Override
    public Optional<AccountBO> resetPasswordByToken(final String token, final String plainPassword) {
        final AccountTokenDO accountToken = accountTokensRepository.getByToken(token)
                .join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                        "AccountDO token " + token + " does not exist"));

        if (accountToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token " + token + " has expired");
        }

        return updatePassword(accountToken.getAssociatedAccountId(), plainPassword);
    }

    @Override
    public Optional<AccountBO> replacePassword(final String identifier,
                                               final String oldPassword,
                                               final String newPassword, final String domain) {
        final AccountBO credentials = accountsService.getByIdentifierUnsafe(identifier, domain)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST, "Unknown identifier"));

        if (!securePassword.verify(oldPassword, credentials.getHashedPassword())) {
            throw new ServiceException(ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords do not match");
        }

        final HashedPasswordBO newHashedPassword = credentialsManager.verifyAndHashPassword(newPassword);
        final AccountBO update = credentials
                .withHashedPassword(newHashedPassword)
                .withPasswordUpdatedAt(Instant.now());

        return doUpdate(credentials, update)
                .map(result -> {
                    storePasswordUpdateRecord(credentials);

                    return result;
                });
    }

    private Optional<AccountBO> doUpdate(final AccountBO existing, final AccountBO updated) {
        storeAuditAttempt(existing);

        return accountsService.update(updated)
                .map(c -> {
                    messageBus.publish(CREDENTIALS_CHANNEL, Messages.updated(c));

                    return c;
                })
                .map(credentialsManager::removeSensitiveInformation);
    }

    private void storeIdentifierUpdateRecord(final AccountBO credentials, final UserIdentifierBO update,
                                             final CredentialsAudit.Action action) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id(ID.generate())
                .credentialsId(credentials.getId())
                .action(action)
                .identifier(update.getIdentifier())
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }

    private void storePasswordUpdateRecord(final AccountBO credentials) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id(ID.generate())
                .credentialsId(credentials.getId())
                .action(CredentialsAudit.Action.UPDATED)
                .password(credentials.getHashedPassword())
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }

    private void storeAuditAttempt(final AccountBO credentials) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id(ID.generate())
                .credentialsId(credentials.getId())
                .action(CredentialsAudit.Action.ATTEMPT)
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }
}
