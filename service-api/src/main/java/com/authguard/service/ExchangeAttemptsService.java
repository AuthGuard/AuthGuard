package com.authguard.service;

import com.authguard.service.model.ExchangeAttemptBO;
import com.authguard.service.model.ExchangeAttemptsQueryBO;

import java.util.Collection;

public interface ExchangeAttemptsService {
    Collection<ExchangeAttemptBO> get(String entityId);

    Collection<ExchangeAttemptBO> find(ExchangeAttemptsQueryBO query);
}
