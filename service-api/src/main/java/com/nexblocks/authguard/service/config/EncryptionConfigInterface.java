package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = EncryptionConfig.class)
public interface EncryptionConfigInterface {
    String getAlgorithm();
    String getPrivateKey();
    String getPublicKey();
}
