package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = ApiKeysConfig.class)
@JsonSerialize(as = ApiKeysConfig.class)
public interface ApiKeysConfigInterface {
    @Value.Default
    default Integer getRandomSize() {
        return 32;
    }

    ApiKeyHashingConfig getHash();
}
