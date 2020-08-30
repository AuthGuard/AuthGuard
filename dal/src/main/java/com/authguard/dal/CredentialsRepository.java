package com.authguard.dal;

import com.authguard.dal.common.Repository;
import com.authguard.dal.model.CredentialsDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CredentialsRepository extends Repository<CredentialsDO> {
    CompletableFuture<Optional<CredentialsDO>> findByIdentifier(String identifier);
}
