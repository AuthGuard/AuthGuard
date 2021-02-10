package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ExchangeAttemptBO;
import com.nexblocks.authguard.service.model.ExchangeAttemptsQueryBO;

import java.util.Collection;

public interface ExchangeAttemptsService extends CrudService<ExchangeAttemptBO> {
    Collection<ExchangeAttemptBO> getByEntityId(String entityId);

    Collection<ExchangeAttemptBO> find(ExchangeAttemptsQueryBO query);
}
