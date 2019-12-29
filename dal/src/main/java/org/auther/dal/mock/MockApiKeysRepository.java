package org.auther.dal.mock;

import org.auther.dal.ApiKeysRepository;
import org.auther.dal.model.ApiKeyDO;

import java.util.Collection;
import java.util.stream.Collectors;

public class MockApiKeysRepository extends AbstractRepository<ApiKeyDO> implements ApiKeysRepository {
    @Override
    public Collection<ApiKeyDO> getByAppId(final String id) {
        return getRepo().values()
                .stream()
                .filter(key -> key.getAppId().equals(id))
                .collect(Collectors.toList());
    }
}
