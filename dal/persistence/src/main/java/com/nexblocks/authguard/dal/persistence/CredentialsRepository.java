package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.CredentialsDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.Optional;
import io.smallrye.mutiny.Uni;

@Deprecated
public interface CredentialsRepository extends Repository<CredentialsDO> {
    Uni<Optional<CredentialsDO>> findByIdentifier(String identifier, final String domain);
}
