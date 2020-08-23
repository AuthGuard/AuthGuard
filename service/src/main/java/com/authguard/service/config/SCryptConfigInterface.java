package com.authguard.service.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = SCryptConfig.class)
public interface SCryptConfigInterface {
    @Value.Default
    @JsonProperty("CPUMemoryCostParameter")
    default Integer getCPUMemoryCostParameter() {
        return 2;
    }

    @Value.Default
    default Integer getBlockSize() {
        return 1;
    }

    @Value.Default
    default Integer getParallelization() {
        return 1;
    }

    @Value.Default
    default Integer getSaltSize() {
        return 32;
    }

    @Value.Default
    default Integer getKeySize() {
        return 50;
    }
}
