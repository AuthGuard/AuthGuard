package com.authguard.dal;

import com.authguard.dal.model.OneTimePasswordDO;

import java.util.Optional;

public interface OtpRepository {
    OneTimePasswordDO save(OneTimePasswordDO password);
    Optional<OneTimePasswordDO> getById(String id);
}
