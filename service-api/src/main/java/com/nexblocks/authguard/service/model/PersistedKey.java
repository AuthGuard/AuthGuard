package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface PersistedKey extends Entity {
    String getDomain();
    Long getAccountId();
    Long getAppId();
    String getAlgorithm();
    int getSize();
    String getPrivateKey();
    String getPublicKey();
}
