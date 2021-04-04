package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonSerialize(as = ApiKeyHashingConfig.class)
@JsonDeserialize(as = ApiKeyHashingConfig.class)
public interface ApiKeyHashingConfigInterface {
    @Value.Default
    default String getAlgorithm() {
        return "blake2b";
    }

    String getKey();

    @Value.Default
    default Integer getDigestSize() {
        return 64;
    }
}
