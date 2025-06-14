package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.CredentialsAuditDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import com.nexblocks.authguard.dal.repository.IndelibleRecordRepository;

import java.util.List;
import io.smallrye.mutiny.Uni;

public interface CredentialsAuditRepository
        extends IndelibleRecordRepository<CredentialsAuditDO>, ImmutableRecordRepository<CredentialsAuditDO> {
    Uni<List<CredentialsAuditDO>> findByCredentialsId(long credentialsId);
}
