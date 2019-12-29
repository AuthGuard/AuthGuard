package org.auther.dal.mock;

import org.auther.dal.ApplicationsRepository;
import org.auther.dal.model.AppDO;

import java.util.List;
import java.util.stream.Collectors;

public class MockApplicationsRepository extends AbstractRepository<AppDO> implements ApplicationsRepository {
    @Override
    public List<AppDO> getAllForAccount(final String accountId) {
        return getRepo().values()
                .stream()
                .filter(app -> app.getParentAccountId().equals(accountId))
                .collect(Collectors.toList());
    }
}
