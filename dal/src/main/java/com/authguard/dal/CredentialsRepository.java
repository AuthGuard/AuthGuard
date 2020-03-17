package com.authguard.dal;

import com.authguard.dal.model.CredentialsDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CredentialsRepository {
    CompletableFuture<CredentialsDO> save(CredentialsDO credentials);
    CompletableFuture<Optional<CredentialsDO>> getById(String id);
    CompletableFuture<Optional<CredentialsDO>> findByUsername(String username);
    CompletableFuture<Optional<CredentialsDO>> update(CredentialsDO credentials);
    CompletableFuture<Optional<CredentialsDO>> delete(String id);
}
