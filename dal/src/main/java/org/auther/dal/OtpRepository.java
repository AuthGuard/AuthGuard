package org.auther.dal;

import org.auther.dal.model.OneTimePasswordDO;

import java.util.Optional;

public interface OtpRepository {
    OneTimePasswordDO save(OneTimePasswordDO password);
    Optional<OneTimePasswordDO> getById(String id);
}
