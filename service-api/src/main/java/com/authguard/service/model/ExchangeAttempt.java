package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface ExchangeAttempt {
     String getEntityId();
     String getExchangeFrom();
     String getExchangeTo();
     boolean isSuccessful();
}
