package com.authguard.dal;

import com.authguard.dal.model.OneTimePasswordDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OtpRepository {
    CompletableFuture<OneTimePasswordDO> save(OneTimePasswordDO password);
    CompletableFuture<Optional<OneTimePasswordDO>> getById(String id);
}
