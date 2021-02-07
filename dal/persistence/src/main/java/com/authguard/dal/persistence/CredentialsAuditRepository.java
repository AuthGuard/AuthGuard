package com.authguard.dal.persistence;

import com.authguard.dal.model.CredentialsAuditDO;
import com.authguard.dal.repository.ImmutableRecordRepository;
import com.authguard.dal.repository.IndelibleRecordRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CredentialsAuditRepository
        extends IndelibleRecordRepository<CredentialsAuditDO>, ImmutableRecordRepository<CredentialsAuditDO> {
    CompletableFuture<List<CredentialsAuditDO>> findByCredentialsId(String credentialsId);
}
