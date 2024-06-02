package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface PersistedKey extends Entity {
    String getDomain();
    Long getAccountId();
    Long getAppId();
    String getName();
    String getAlgorithm();
    int getSize();
    int getVersion();
    String getPasscode();
    boolean isPasscodeProtected();
    String getPasscodeCheckPlain();
    String getPasscodeCheckEncrypted();
    byte[] getNonce();
    String getPrivateKey();
    String getPublicKey();
}
