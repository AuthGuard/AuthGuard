package com.authguard.service;

import com.authguard.service.model.ExchangeAttemptBO;

import java.util.Collection;

public interface ExchangeAttemptsService {
    Collection<ExchangeAttemptBO> get(String entityId);
}
