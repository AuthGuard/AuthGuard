package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface TotpKey extends Entity {
    long getAccountId();
    String getAuthenticator();
    byte[] getNonce();
    byte[] getKey();
    String getQrCode();
}
