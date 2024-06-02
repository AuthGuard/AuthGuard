package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = CryptoKeyConfig.class)
public interface CryptoKeyConfigInterface {
    @Value.Default
    default Integer getVersion() {
        return 1;
    }

    String getEncryptionKey();

    List<CryptoKeyConfig> getPreviousVersion();
}
