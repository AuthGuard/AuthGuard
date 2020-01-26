package com.authguard.dal;

import com.authguard.dal.model.CredentialsDO;

import java.util.Optional;

public interface CredentialsRepository {
    CredentialsDO save(CredentialsDO credentials);
    Optional<CredentialsDO> getById(String id);
    Optional<CredentialsDO> findByUsername(String username);
    Optional<CredentialsDO> update(CredentialsDO credentials);
    Optional<CredentialsDO> delete(String id);
}
