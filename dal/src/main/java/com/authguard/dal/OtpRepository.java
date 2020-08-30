package com.authguard.dal;

import com.authguard.dal.common.ImmutableRecordRepository;
import com.authguard.dal.common.IndelibleRecordRepository;
import com.authguard.dal.model.OneTimePasswordDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OtpRepository extends IndelibleRecordRepository<OneTimePasswordDO>, ImmutableRecordRepository<OneTimePasswordDO> {
}
