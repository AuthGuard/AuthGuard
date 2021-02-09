package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Default configuration was based on Practical
 * Cryptography for Developers recommendations
 * for interactive login setup.
 * Ref: https://cryptobook.nakov.com/mac-and-key-derivation/scrypt#scrypt-parameters
 */
@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = SCryptConfig.class)
public interface SCryptConfigInterface {
    @Value.Default
    @JsonProperty("CPUMemoryCostParameter")
    default Integer getCPUMemoryCostParameter() {
        return 16384;
    }

    @Value.Default
    default Integer getBlockSize() {
        return 8;
    }

    @Value.Default
    default Integer getParallelization() {
        return 1;
    }

    @Value.Default
    default Integer getSaltSize() {
        return 16;
    }

    @Value.Default
    default Integer getKeySize() {
        return 50;
    }
}
