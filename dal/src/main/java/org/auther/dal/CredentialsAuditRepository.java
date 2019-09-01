package org.auther.dal;

import org.auther.dal.model.CredentialsAuditDO;

import java.util.List;
import java.util.Optional;

public interface CredentialsAuditRepository {
    CredentialsAuditDO save(CredentialsAuditDO credentialsAudit);
    Optional<CredentialsAuditDO> getById(String id);
    List<CredentialsAuditDO> findByCredentialsId(String credentialsId);
}
