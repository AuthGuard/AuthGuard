package com.authguard.api.dto.entities;

import com.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
public interface ExchangeAttempt {
    String getEntityId();
    String getExchangeFrom();
    String getExchangeTo();
    boolean isSuccessful();
}
