package com.authguard.service.impl;

import com.authguard.dal.model.CredentialsDO;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.*;
import com.google.inject.Inject;
import com.authguard.dal.CredentialsAuditRepository;
import com.authguard.dal.CredentialsRepository;
import com.authguard.service.CredentialsService;
import com.authguard.service.passwords.SecurePassword;

import java.util.Optional;
import java.util.UUID;

public class CredentialsServiceImpl implements CredentialsService {
    private static final String CREDENTIALS_CHANNEL = "credentials";

    private final AccountsService accountsService;
    private final CredentialsRepository credentialsRepository;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final SecurePassword securePassword;
    private final MessageBus messageBus;
    private final ServiceMapper serviceMapper;

    @Inject
    public CredentialsServiceImpl(final AccountsService accountsService,
                                  final CredentialsRepository credentialsRepository,
                                  final CredentialsAuditRepository credentialsAuditRepository,
                                  final SecurePassword securePassword,
                                  final MessageBus messageBus,
                                  final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.credentialsRepository = credentialsRepository;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.securePassword = securePassword;
        this.messageBus = messageBus;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public CredentialsBO create(final CredentialsBO credentials) {
        ensureAccountExists(credentials.getAccountId());
        credentials.getIdentifiers()
                .forEach(identifier -> ensureNoDuplicate(identifier.getIdentifier()));

        final HashedPasswordBO hashedPassword = securePassword.hash(credentials.getPlainPassword());
        final CredentialsBO credentialsHashedPassword = CredentialsBO.builder()
                .from(credentials)
                .hashedPassword(hashedPassword)
                .id(UUID.randomUUID().toString())
                .build();

        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsHashedPassword);

        return credentialsRepository.save(credentialsDO)
                .thenApply(serviceMapper::toBO)
                .thenApply(this::removeSensitiveInformation)
                .thenApply(created -> {
                    messageBus.publish(CREDENTIALS_CHANNEL, Messages.created(created));
                    return created;
                })
                .join();
    }

    @Override
    public Optional<CredentialsBO> getById(final String id) {
        return credentialsRepository.getById(id)
                .thenApply(optional -> optional.map(serviceMapper::toBO).map(this::removeSensitiveInformation))
                .join();
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
        return doUpdate(credentials.withPlainPassword(null).withHashedPassword(null), false);
    }

    @Override
    public Optional<CredentialsBO> updatePassword(final CredentialsBO credentials) {
        final HashedPasswordBO newPassword = securePassword.hash(credentials.getPlainPassword());
        final CredentialsBO update = credentials.withHashedPassword(newPassword);

        return doUpdate(update, true);
    }

    private Optional<CredentialsBO> doUpdate(final CredentialsBO credentials, boolean storePasswordAudit) {
        credentials.getIdentifiers()
                .forEach(identifier -> ensureNoDuplicate(identifier.getIdentifier(), credentials.getAccountId()));

        final CredentialsBO existing = credentialsRepository.getById(credentials.getId())
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .orElseThrow(ServiceNotFoundException::new);

        final CredentialsBO update = credentials.getHashedPassword() == null ?
                credentials.withHashedPassword(existing.getHashedPassword()) : credentials;

        // regardless of whether storePasswordAudit is true or not, we don't need the password in attempt stage
        storeAuditRecord(removeSensitiveInformation(existing), removeSensitiveInformation(credentials),
                CredentialsAudit.Action.ATTEMPT);

        return credentialsRepository.update(serviceMapper.toDO(update))
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .map(c -> {
                    if (storePasswordAudit) {
                        storeAuditRecord(existing, credentials, CredentialsAudit.Action.UPDATED);
                    } else {
                        storeAuditRecord(removeSensitiveInformation(existing), removeSensitiveInformation(credentials),
                                CredentialsAudit.Action.UPDATED);
                    }
                    return c;
                })
                .map(this::removeSensitiveInformation)
                .map(updated -> {
                    messageBus.publish(CREDENTIALS_CHANNEL, Messages.updated(updated));
                    return updated;
                });
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

    private CredentialsBO removeSensitiveInformation(final CredentialsBO credentials) {
        return credentials.withPlainPassword(null)
                .withHashedPassword(null);
    }

    private void storeAuditRecord(final CredentialsBO credentials, final CredentialsBO update,
                                  final CredentialsAudit.Action action) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id("")
                .credentialsId(credentials.getId())
                .action(action)
                .before(credentials)
                .after(update)
                .password(credentials.getHashedPassword())
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }

    // TODO should be done by the DB
    private void ensureNoDuplicate(final String identifier) {
        credentialsRepository.findByIdentifier(identifier)
                .join()
                .ifPresent(ignored -> { throw new ServiceConflictException("Username already exists"); });
    }

    // TODO should be done by the DB
    private void ensureNoDuplicate(final String identifier, final String accountId) {
        credentialsRepository.findByIdentifier(identifier)
                .join()
                .filter(credentials -> !credentials.getAccountId().equals(accountId))
                .ifPresent(ignored -> { throw new ServiceConflictException("Username already exists"); });
    }

    private void ensureAccountExists(final String accountId) {
        if (accountsService.getById(accountId).isEmpty()) {
            throw new ServiceException("No account with ID " + accountId + " exists");
        }
    }
}
