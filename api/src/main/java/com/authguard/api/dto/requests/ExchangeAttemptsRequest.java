package com.authguard.api.dto.requests;

import com.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
public interface ExchangeAttemptsRequest {
    String getEntityId();
    String getFromExchange();
    String getFromTimestamp();
}
