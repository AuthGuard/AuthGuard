package com.authguard.service.model;

import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@BOStyle
public interface ExchangeAttemptsQuery {
    String getEntityId();
    String getFromExchange();
    OffsetDateTime getFromTimestamp();
}
