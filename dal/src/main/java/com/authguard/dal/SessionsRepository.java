package com.authguard.dal;

import com.authguard.dal.common.ImmutableRecordRepository;
import com.authguard.dal.common.IndelibleRecordRepository;
import com.authguard.dal.model.SessionDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SessionsRepository extends IndelibleRecordRepository<SessionDO>, ImmutableRecordRepository<SessionDO> {
}
