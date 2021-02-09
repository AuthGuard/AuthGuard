package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = PasswordsConfig.class)
public interface PasswordsConfigInterface {
    String getAlgorithm();

    @Value.Default
    default SCryptConfig getScrypt() {
        return SCryptConfig.builder().build();
    }

    @Value.Default
    default BCryptConfig getBcrypt() {
        return BCryptConfig.builder().build();
    }

    @Value.Default
    default PasswordConditions getConditions() {
        return PasswordConditions.builder().build();
    }
}
