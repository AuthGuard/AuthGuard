package org.auther.dal.mock;

import org.auther.dal.CredentialsAuditRepository;
import org.auther.dal.model.CredentialsAuditDO;

import java.util.List;
import java.util.stream.Collectors;

public class MockCredentialsAuditRepository extends AbstractRepository<CredentialsAuditDO>
        implements CredentialsAuditRepository {

    @Override
    public List<CredentialsAuditDO> findByCredentialsId(final String credentialsId) {
        return getRepo().values()
                .stream()
                .filter(record -> record.getCredentialId().equals(credentialsId))
                .collect(Collectors.toList());
    }
}
