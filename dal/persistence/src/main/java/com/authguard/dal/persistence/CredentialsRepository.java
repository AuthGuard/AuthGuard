package com.authguard.dal.persistence;

import com.authguard.dal.model.CredentialsDO;
import com.authguard.dal.repository.Repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CredentialsRepository extends Repository<CredentialsDO> {
    CompletableFuture<Optional<CredentialsDO>> findByIdentifier(String identifier);
}
