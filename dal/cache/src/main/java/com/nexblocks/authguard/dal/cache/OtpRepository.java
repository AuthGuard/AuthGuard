package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.OneTimePasswordDO;
import io.smallrye.mutiny.Uni;

import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface OtpRepository {
    Uni<OneTimePasswordDO> save(OneTimePasswordDO password);
    Uni<Optional<OneTimePasswordDO>> getById(long id);
}
