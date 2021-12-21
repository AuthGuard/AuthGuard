package com.nexblocks.authguard.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwords.*;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.model.CredentialsDO;
import com.nexblocks.authguard.dal.persistence.CredentialsAuditRepository;
import com.nexblocks.authguard.dal.persistence.CredentialsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.CredentialsService;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.messaging.ResetTokenMessage;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CredentialsServiceImpl implements CredentialsService {
    private static final String CREDENTIALS_CHANNEL = "credentials";
    private static final int RESET_TOKEN_SIZE = 128;
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final AccountsService accountsService;
    private final IdempotencyService idempotencyService;
    private final CredentialsRepository credentialsRepository;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final AccountTokensRepository accountTokensRepository;
    private final SecurePassword securePassword;
    private final PasswordValidator passwordValidator;
    private final MessageBus messageBus;
    private final ServiceMapper serviceMapper;

    private final CryptographicRandom cryptographicRandom;
    private final PersistenceService<CredentialsBO, CredentialsDO, CredentialsRepository> persistenceService;

    @Inject
    public CredentialsServiceImpl(final AccountsService accountsService,
                                  final IdempotencyService idempotencyService,
                                  final CredentialsRepository credentialsRepository,
                                  final CredentialsAuditRepository credentialsAuditRepository,
                                  final AccountTokensRepository accountTokensRepository,
                                  final SecurePasswordProvider securePasswordProvider,
                                  final PasswordValidator passwordValidator,
                                  final MessageBus messageBus,
                                  final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.idempotencyService = idempotencyService;
        this.credentialsRepository = credentialsRepository;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.accountTokensRepository = accountTokensRepository;
        this.securePassword = securePasswordProvider.get();
        this.passwordValidator = passwordValidator;
        this.messageBus = messageBus;
        this.serviceMapper = serviceMapper;

        this.cryptographicRandom = new CryptographicRandom();

        this.persistenceService = new PersistenceService<>(credentialsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, CREDENTIALS_CHANNEL);
    }

    @Override
    public CredentialsBO create(final CredentialsBO credentials, final RequestContextBO requestContext) {
        return idempotencyService.performOperation(() -> doCreate(credentials), requestContext.getIdempotentKey(), credentials.getEntityType())
                .join();
    }

    private CredentialsBO doCreate(final CredentialsBO credentials) {
        ensureAccountExists(credentials.getAccountId());

        final HashedPasswordBO hashedPassword = verifyAndHashPassword(credentials.getPlainPassword());
        final CredentialsBO credentialsHashedPassword = CredentialsBO.builder()
                .from(credentials)
                .hashedPassword(hashedPassword)
                .id(ID.generate())
                .passwordUpdatedAt(OffsetDateTime.now())
                .build();

        return removeSensitiveInformation(persistenceService.create(credentialsHashedPassword));
    }

    @Override
    public Optional<CredentialsBO> getById(final String id) {
        return persistenceService.getById(id)
                .map(this::removeSensitiveInformation);
    }

    @Override
    public Optional<CredentialsBO> getByIdUnsafe(final String id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<CredentialsBO> getByUsername(final String username) {
        return credentialsRepository.findByIdentifier(username)
                .join()
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation);
    }

    @Override
    public Optional<CredentialsBO> getByUsernameUnsafe(final String username) {
        return credentialsRepository.findByIdentifier(username)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<CredentialsBO> update(final CredentialsBO credentials) {
        throw new UnsupportedOperationException("Regular updates are not supported for credentials. Use updatePassword, addIdentifiers, or removeIdentifiers");
    }

    @Override
    public Optional<CredentialsBO> updatePassword(final String id, final String plainPassword) {
        final CredentialsBO existing = getByIdUnsafe(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final HashedPasswordBO newPassword = verifyAndHashPassword(plainPassword);
        final CredentialsBO update = existing
                .withHashedPassword(newPassword)
                .withPasswordUpdatedAt(OffsetDateTime.now());

        return doUpdate(existing, update, true);
    }

    @Override
    public Optional<CredentialsBO> addIdentifiers(final String id, final List<UserIdentifierBO> identifiers) {
        final CredentialsBO existing = getByIdUnsafe(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final Set<String> existingIdentifiers = existing.getIdentifiers().stream()
                .map(UserIdentifierBO::getIdentifier)
                .collect(Collectors.toSet());

        final List<UserIdentifierBO> combined = new ArrayList<>(existing.getIdentifiers());

        for (final UserIdentifierBO identifier : identifiers) {
            if (existingIdentifiers.contains(identifier.getIdentifier())) {
                throw new ServiceConflictException(ErrorCode.IDENTIFIER_ALREADY_EXISTS, "Duplicate identifier for " + id);
            }

            combined.add(identifier.withActive(true));
        }

        final CredentialsBO updated = CredentialsBO.builder().from(existing)
                .identifiers(combined)
                .build();

        return doUpdate(existing, updated, false);
    }

    @Override
    public Optional<CredentialsBO> removeIdentifiers(final String id, final List<String> identifiers) {
        final CredentialsBO existing = getByIdUnsafe(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final List<UserIdentifierBO> updatedIdentifiers = existing.getIdentifiers().stream()
                .map(identifier -> {
                    if (identifiers.contains(identifier.getIdentifier())) {
                        return identifier.withActive(false);
                    }

                    return identifier;
                })
                .collect(Collectors.toList());

        final CredentialsBO updated = CredentialsBO.builder().from(existing)
                .identifiers(updatedIdentifiers)
                .build();

        return doUpdate(existing, updated, false);
    }

    @Override
    public Optional<CredentialsBO> replaceIdentifier(final String id, final String oldIdentifier, final UserIdentifierBO newIdentifier) {
        final CredentialsBO credentials = getByIdUnsafe(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final boolean hasIdentifier = credentials.getIdentifiers()
                .stream()
                .anyMatch(identifier -> identifier.getIdentifier().equals(oldIdentifier));

        if (!hasIdentifier) {
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
                                .build();
                    }

                    return identifier;
                })
                .collect(Collectors.toSet());

        final CredentialsBO update = credentials.withIdentifiers(newIdentifiers);

        return doUpdate(credentials, update, false);
    }

    @Override
    public PasswordResetTokenBO generateResetToken(final String identifier, final boolean returnToken) {
        final CredentialsBO credentials = getByUsername(identifier)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST, "Unknown identifier"));
        final AccountBO account = accountsService.getById(credentials.getAccountId())
                .orElseThrow(() -> new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Credentials found for the identifier but no account was associated with it. This could be the " +
                                "result of deleting an account without deleting its credentials"));

        final OffsetDateTime now = OffsetDateTime.now();

        final AccountTokenDO accountToken = AccountTokenDO
                .builder()
                .id(ID.generate())
                .token(cryptographicRandom.base64Url(RESET_TOKEN_SIZE))
                .associatedAccountId(account.getId())
                .additionalInformation(ImmutableMap.of("credentialsId", credentials.getId()))
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .build();

        accountTokensRepository.save(accountToken).join();

        messageBus.publish(CREDENTIALS_CHANNEL,
                Messages.resetTokenGenerated(new ResetTokenMessage(account, accountToken)));

        return PasswordResetTokenBO.builder()
                .token(returnToken ? accountToken.getToken() : null)
                .issuedAt(now.toEpochSecond())
                .expiresAt(accountToken.getExpiresAt().toEpochSecond())
                .build();
    }

    @Override
    public Optional<CredentialsBO> resetPassword(final String token, final String plainPassword) {
        final AccountTokenDO accountToken = accountTokensRepository.getByToken(token)
                .join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                        "AccountDO token " + token + " does not exist"));

        if (accountToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token " + token + " has expired");
        }

        final String credentialsId = Optional.ofNullable(accountToken.getAdditionalInformation())
                .map(m -> m.get("credentialsId"))
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_TOKEN, "Reset token was not mapped to any credentials"));

        return updatePassword(credentialsId, plainPassword);
    }

    private Optional<CredentialsBO> doUpdate(final CredentialsBO existing, final CredentialsBO updated, boolean storePasswordAudit) {
        storeAuditRecord(removeSensitiveInformation(existing), removeSensitiveInformation(updated),
                CredentialsAudit.Action.ATTEMPT);

        return persistenceService.update(updated)
                .map(c -> {
                    if (storePasswordAudit) {
                        storeAuditRecord(existing, updated, CredentialsAudit.Action.UPDATED);
                    } else {
                        storeAuditRecord(removeSensitiveInformation(existing), removeSensitiveInformation(updated),
                                CredentialsAudit.Action.UPDATED);
                    }
                    return c;
                })
                .map(this::removeSensitiveInformation);
    }

    @Override
    public Optional<CredentialsBO> delete(final String id) {
        return credentialsRepository.delete(id)
                .thenApply(optional -> optional
                        .map(serviceMapper::toBO)
                        .map(this::removeSensitiveInformation)
                        .map(deleted -> {
                            messageBus.publish(CREDENTIALS_CHANNEL, Messages.deleted(deleted));
                            return deleted;
                        })
                )
                .join();
    }

    private HashedPasswordBO verifyAndHashPassword(final String plain) {
        final List<Violation> passwordViolations = passwordValidator.findViolations(plain);

        if (!passwordViolations.isEmpty()) {
            throw new ServiceInvalidPasswordException(passwordViolations);
        }

        return securePassword.hash(plain);
    }

    private CredentialsBO removeSensitiveInformation(final CredentialsBO credentials) {
        return credentials.withPlainPassword(null)
                .withHashedPassword(null);
    }

    private void storeAuditRecord(final CredentialsBO credentials, final CredentialsBO update,
                                  final CredentialsAudit.Action action) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id(ID.generate())
                .credentialsId(credentials.getId())
                .action(action)
                .before(credentials)
                .after(update)
                .password(credentials.getHashedPassword())
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }

    private void ensureAccountExists(final String accountId) {
        if (accountsService.getById(accountId).isEmpty()) {
            throw new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + accountId + " exists");
        }
    }
}
