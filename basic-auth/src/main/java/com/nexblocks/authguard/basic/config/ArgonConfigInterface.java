package com.nexblocks.authguard.basic.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.config.ConfigStyle;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = ArgonConfig.class)
public interface ArgonConfigInterface {
    @Value.Default
    default Integer getSaltSize() {
        return 16;
    }

    @Value.Default
    default Integer getIterations() {
        return 2;
    }

    @Value.Default
    default Integer getParallelism() {
        return 1;
    }

    @Value.Default
    default Integer getMemoryLimit() {
        return 66536;
    }
}
