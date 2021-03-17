package com.nexblocks.authguard.basic.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.config.ConfigStyle;
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
