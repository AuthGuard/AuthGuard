package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface PasswordResetToken {
    String getToken();
    long getIssuedAt();
    long getExpiresAt();
}
