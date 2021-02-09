package com.nexblocks.authguard.extensions.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        validationMethod = Value.Style.ValidationMethod.NONE
)
@JsonDeserialize(as = ImmutableAccountLockerConfig.class)
@JsonSerialize(as = ImmutableAccountLockerConfig.class)
public interface AccountLockerConfig {
    @Value.Default
    default Integer getMaxAttempts() {
        return 3;
    }

    @Value.Default
    default Integer getCheckPeriod() {
        return 30;
    }

    @Value.Default
    default Integer getLockPeriod() {
        return 60;
    }
}
