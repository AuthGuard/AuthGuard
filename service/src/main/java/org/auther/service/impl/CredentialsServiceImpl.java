package org.auther.service.impl;

import com.google.inject.Inject;
import org.auther.dal.CredentialsAuditRepository;
import org.auther.dal.CredentialsRepository;
import org.auther.service.CredentialsService;
import org.auther.service.SecurePassword;
import org.auther.service.exceptions.ServiceNotFoundException;
import org.auther.service.model.CredentialsAudit;
import org.auther.service.model.CredentialsAuditBO;
import org.auther.service.model.CredentialsBO;
import org.auther.service.model.HashedPasswordBO;

import java.util.Optional;

public class CredentialsServiceImpl implements CredentialsService {
    private final CredentialsRepository credentialsRepository;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final SecurePassword securePassword;
    private final ServiceMapper serviceMapper;

    @Inject
    public CredentialsServiceImpl(final CredentialsRepository credentialsRepository, final CredentialsAuditRepository credentialsAuditRepository,
                                  final SecurePassword securePassword, final ServiceMapper serviceMapper) {
        this.credentialsRepository = credentialsRepository;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.securePassword = securePassword;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public CredentialsBO create(final CredentialsBO credentials) {
        final HashedPasswordBO hashedPassword = securePassword.hash(credentials.getPlainPassword());

        return Optional.of(credentials.withHashedPassword(hashedPassword))
                .map(serviceMapper::toDO)
                .map(credentialsRepository::save)
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Optional<CredentialsBO> getById(final String id) {
        return credentialsRepository.getById(id)
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation);
    }

    @Override
    public Optional<CredentialsBO> getByUsername(final String username) {
        return credentialsRepository.findByUsername(username)
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation);
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
        final CredentialsBO existing = credentialsRepository.getById(credentials.getId())
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final CredentialsBO update = credentials.getHashedPassword() == null ?
                credentials.withHashedPassword(existing.getHashedPassword()) : credentials;

        // regardless of whether storePasswordAudi is true or not, we don't need the password in attempt stage
        storeAuditRecord(removeSensitiveInformation(existing), CredentialsAudit.Action.ATTEMPT);

        return credentialsRepository.update(serviceMapper.toDO(update))
                .map(serviceMapper::toBO)
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
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation);
    }

    private CredentialsBO removeSensitiveInformation(final CredentialsBO credentials) {
        return credentials.withPlainPassword(null)
                .withHashedPassword(null);
    }

    private CredentialsBO storeAuditRecord(final CredentialsBO credentials, final CredentialsAudit.Action action) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id("")
                .credentialId(credentials.getId())
                .action(action)
                .username(credentials.getUsername())
                .password(credentials.getHashedPassword())
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));

        return credentials;
    }
}
