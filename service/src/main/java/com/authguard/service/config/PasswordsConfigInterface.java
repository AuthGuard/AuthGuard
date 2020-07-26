package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = PasswordsConfig.class)
public interface PasswordsConfigInterface {
    String getAlgorithm();

    @Value.Default
    default PasswordConditions getConditions() {
        return PasswordConditions.builder().build();
    }
}
