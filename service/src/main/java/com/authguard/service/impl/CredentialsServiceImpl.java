package com.authguard.service.impl;

import com.authguard.dal.model.CredentialsDO;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.impl.mappers.ServiceMapper;
import com.authguard.service.model.CredentialsAudit;
import com.google.inject.Inject;
import com.authguard.dal.CredentialsAuditRepository;
import com.authguard.dal.CredentialsRepository;
import com.authguard.service.CredentialsService;
import com.authguard.service.SecurePassword;
import com.authguard.service.model.CredentialsAuditBO;
import com.authguard.service.model.CredentialsBO;
import com.authguard.service.model.HashedPasswordBO;

import java.util.Optional;
import java.util.UUID;

public class CredentialsServiceImpl implements CredentialsService {
    private final AccountsService accountsService;
    private final CredentialsRepository credentialsRepository;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final SecurePassword securePassword;
    private final ServiceMapper serviceMapper;

    @Inject
    public CredentialsServiceImpl(final AccountsService accountsService,
                                  final CredentialsRepository credentialsRepository,
                                  final CredentialsAuditRepository credentialsAuditRepository,
                                  final SecurePassword securePassword,
                                  final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.credentialsRepository = credentialsRepository;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.securePassword = securePassword;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public CredentialsBO create(final CredentialsBO credentials) {
        ensureAccountExists(credentials.getAccountId());
        ensureNoDuplicate(credentials);

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
        return credentialsRepository.findByUsername(username)
                .thenApply(optional -> optional.map(serviceMapper::toBO).map(this::removeSensitiveInformation))
                .join();
    }

    @Override
    public Optional<CredentialsBO> getByUsernameUnsafe(final String username) {
        return credentialsRepository.findByUsername(username)
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
        ensureNoDuplicate(credentials);

        final CredentialsBO existing = credentialsRepository.getById(credentials.getId())
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .orElseThrow(ServiceNotFoundException::new);

        final CredentialsBO update = credentials.getHashedPassword() == null ?
                credentials.withHashedPassword(existing.getHashedPassword()) : credentials;

        // regardless of whether storePasswordAudi is true or not, we don't need the password in attempt stage
        storeAuditRecord(removeSensitiveInformation(existing), CredentialsAudit.Action.ATTEMPT);

        return credentialsRepository.update(serviceMapper.toDO(update))
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .map(c -> {
                    if (storePasswordAudit) {
                        storeAuditRecord(existing, CredentialsAudit.Action.UPDATED);
                    } else {
                        storeAuditRecord(removeSensitiveInformation(existing), CredentialsAudit.Action.UPDATED);
                    }
                    return c;
                })
                .map(this::removeSensitiveInformation);
    }

    @Override
    public Optional<CredentialsBO> delete(final String id) {
        return credentialsRepository.delete(id)
                .thenApply(optional -> optional.map(serviceMapper::toBO).map(this::removeSensitiveInformation))
                .join();
    }

    private CredentialsBO removeSensitiveInformation(final CredentialsBO credentials) {
        return credentials.withPlainPassword(null)
                .withHashedPassword(null);
    }

    private void storeAuditRecord(final CredentialsBO credentials, final CredentialsAudit.Action action) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id("")
                .credentialId(credentials.getId())
                .action(action)
                .username(credentials.getUsername())
                .password(credentials.getHashedPassword())
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }

    private void ensureNoDuplicate(final CredentialsBO credentials) {
        credentialsRepository.findByUsername(credentials.getUsername())
                .ifPresent(ignored -> { throw new ServiceConflictException("Username already exists"); });
    }

    private void ensureAccountExists(final String accountId) {
        if (accountsService.getById(accountId).isEmpty()) {
            throw new ServiceException("No account with ID " + accountId + " exists");
        }
    }
}
