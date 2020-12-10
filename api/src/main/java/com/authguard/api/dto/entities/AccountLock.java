package com.authguard.api.dto.entities;

import com.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@DTOStyle
public interface AccountLock {
    String getAccountId();
    OffsetDateTime getExpiresAt();
}
