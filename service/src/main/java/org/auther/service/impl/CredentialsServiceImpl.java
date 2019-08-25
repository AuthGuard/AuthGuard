package org.auther.service.impl;

import com.google.inject.Inject;
import org.auther.dal.CredentialsRepository;
import org.auther.service.CredentialsService;
import org.auther.service.SecurePassword;
import org.auther.service.model.CredentialsBO;
import org.auther.service.model.HashedPasswordBO;

import java.util.Optional;

public class CredentialsServiceImpl implements CredentialsService {
    private final CredentialsRepository credentialsRepository;
    private final SecurePassword securePassword;
    private final ServiceMapper serviceMapper;

    @Inject
    public CredentialsServiceImpl(final CredentialsRepository credentialsRepository, final SecurePassword securePassword,
                                  final ServiceMapper serviceMapper) {
        this.credentialsRepository = credentialsRepository;
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
    public Optional<CredentialsBO> update(final CredentialsBO credentialsBO) {
        return credentialsRepository.update(serviceMapper.toDO(credentialsBO))
                .map(serviceMapper::toBO)
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
}
