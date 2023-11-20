package com.nexblocks.authguard.basic.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.config.ConfigStyle;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = Pbkdf2Config.class)
public interface Pbkdf2ConfigInterface {
    @Value.Default
    default Integer getSaltSize() {
        return 16;
    }

    Pkdf2HashingAlgorithm getHashingAlgorithm();

    @Value.Default
    default int getIterations() {
        switch (getHashingAlgorithm()) {
            case SHA_256: return 600_000;
            case SHA_512: return 210_00;
            default: return -1;
        }
    }

    enum Pkdf2HashingAlgorithm {
        SHA_256,
        SHA_512
    }
}
