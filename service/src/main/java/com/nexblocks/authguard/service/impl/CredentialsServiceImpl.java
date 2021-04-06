package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwords.PasswordValidator;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.ServiceInvalidPasswordException;
import com.nexblocks.authguard.basic.passwords.Violation;
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
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CredentialsServiceImpl implements CredentialsService {
    private static final String CREDENTIALS_CHANNEL = "credentials";

    private final AccountsService accountsService;
    private final IdempotencyService idempotencyService;
    private final CredentialsRepository credentialsRepository;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final SecurePassword securePassword;
    private final PasswordValidator passwordValidator;
    private final MessageBus messageBus;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<CredentialsBO, CredentialsDO, CredentialsRepository> persistenceService;

    @Inject
    public CredentialsServiceImpl(final AccountsService accountsService,
                                  final IdempotencyService idempotencyService,
                                  final CredentialsRepository credentialsRepository,
                                  final CredentialsAuditRepository credentialsAuditRepository,
                                  final SecurePassword securePassword,
                                  final PasswordValidator passwordValidator,
                                  final MessageBus messageBus,
                                  final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.idempotencyService = idempotencyService;
        this.credentialsRepository = credentialsRepository;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.securePassword = securePassword;
        this.passwordValidator = passwordValidator;
        this.messageBus = messageBus;
        this.serviceMapper = serviceMapper;

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
                .build();

        return removeSensitiveInformation(persistenceService.create(credentialsHashedPassword));
    }

    @Override
    public Optional<CredentialsBO> getById(final String id) {
        return persistenceService.getById(id)
                .map(this::removeSensitiveInformation);
    }

    @Override
    public Optional<CredentialsBO> getByUsername(final String username) {
        return credentialsRepository.findByIdentifier(username)
                .thenApply(optional -> optional.map(serviceMapper::toBO).map(this::removeSensitiveInformation))
                .join();
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
        final CredentialsBO existing = persistenceService.getById(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final HashedPasswordBO newPassword = verifyAndHashPassword(plainPassword);
        final CredentialsBO update = existing.withHashedPassword(newPassword);

        return doUpdate(existing, update, true);
    }

    @Override
    public Optional<CredentialsBO> addIdentifiers(final String id, final List<UserIdentifierBO> identifiers) {
        final CredentialsBO existing = getById(id)
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
        final CredentialsBO existing = getById(id)
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
