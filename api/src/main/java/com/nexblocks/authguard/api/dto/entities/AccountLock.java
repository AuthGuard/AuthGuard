package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@DTOStyle
public interface AccountLock {
    String getAccountId();
    OffsetDateTime getExpiresAt();
}
