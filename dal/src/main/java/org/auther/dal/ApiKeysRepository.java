package org.auther.dal;

import org.auther.dal.model.ApiKeyDO;

import java.util.Collection;
import java.util.Optional;

public interface ApiKeysRepository {
    ApiKeyDO save(ApiKeyDO apiKey);
    Optional<ApiKeyDO> getById(String id);
    Collection<ApiKeyDO> getByAppId(String id);
    Optional<ApiKeyDO> update(ApiKeyDO apiKey);
    Optional<ApiKeyDO> delete(String id);
}
