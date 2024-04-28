package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface EphemeralKey {
    String getAlgorithm();
    int getSize();
    String getPrivateKey();
    String getPublicKey();
}
