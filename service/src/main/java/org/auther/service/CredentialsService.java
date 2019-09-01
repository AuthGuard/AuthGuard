package org.auther.service;

import org.auther.service.model.CredentialsBO;

import java.util.Optional;

public interface CredentialsService {
    CredentialsBO create(CredentialsBO credentials);
    Optional<CredentialsBO> getById(String id);
    Optional<CredentialsBO> getByUsername(String username);
    Optional<CredentialsBO> update(CredentialsBO credentials);
    Optional<CredentialsBO> updatePassword(CredentialsBO credentials);
    Optional<CredentialsBO> delete(String id);
}
