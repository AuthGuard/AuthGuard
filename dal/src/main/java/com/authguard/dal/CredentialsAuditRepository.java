package com.authguard.dal;

import com.authguard.dal.model.CredentialsAuditDO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CredentialsAuditRepository {
    CompletableFuture<CredentialsAuditDO> save(CredentialsAuditDO credentialsAudit);
    CompletableFuture<Optional<CredentialsAuditDO>> getById(String id);
    CompletableFuture<List<CredentialsAuditDO>> findByCredentialsId(String credentialsId);
}
