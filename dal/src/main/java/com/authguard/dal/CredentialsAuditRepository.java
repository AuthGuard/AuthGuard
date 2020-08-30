package com.authguard.dal;

import com.authguard.dal.common.ImmutableRecordRepository;
import com.authguard.dal.common.IndelibleRecordRepository;
import com.authguard.dal.model.CredentialsAuditDO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CredentialsAuditRepository extends
        IndelibleRecordRepository<CredentialsAuditDO>, ImmutableRecordRepository<CredentialsAuditDO> {
    CompletableFuture<List<CredentialsAuditDO>> findByCredentialsId(String credentialsId);
}
