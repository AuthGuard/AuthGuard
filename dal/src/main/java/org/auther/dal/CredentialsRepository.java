package org.auther.dal;

import org.auther.dal.model.CredentialsDO;

import java.util.Optional;

public interface CredentialsRepository {
    CredentialsDO save(CredentialsDO credentials);
    Optional<CredentialsDO> getById(String id);
    Optional<CredentialsDO> findByUsername(String username);
    Optional<CredentialsDO> update(CredentialsDO credentials);
    Optional<CredentialsDO> delete(String id);
}
