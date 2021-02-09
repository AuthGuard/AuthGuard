package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = BCryptConfig.class)
public interface BCryptConfigInterface {
    @Value.Default
    default Integer getCost() {
        return 4;
    }

    @Value.Default
    default Integer getSaltSize() {
        return 16;
    }
}
