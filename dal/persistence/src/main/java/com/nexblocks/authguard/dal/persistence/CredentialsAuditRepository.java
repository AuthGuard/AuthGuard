package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.CredentialsAuditDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import com.nexblocks.authguard.dal.repository.IndelibleRecordRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CredentialsAuditRepository
        extends IndelibleRecordRepository<CredentialsAuditDO>, ImmutableRecordRepository<CredentialsAuditDO> {
    CompletableFuture<List<CredentialsAuditDO>> findByCredentialsId(long credentialsId);
}
