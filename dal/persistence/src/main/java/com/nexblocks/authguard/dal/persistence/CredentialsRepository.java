package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.CredentialsDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Deprecated
public interface CredentialsRepository extends Repository<CredentialsDO> {
    CompletableFuture<Optional<CredentialsDO>> findByIdentifier(String identifier, final String domain);
}
