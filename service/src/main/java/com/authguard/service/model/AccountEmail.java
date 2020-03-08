package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface AccountEmail {
    String getEmail();
    boolean isVerified();
    boolean isPrimary();
    boolean isBackup();
}
