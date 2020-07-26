package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = PasswordConditions.class)
public interface PasswordConditionsInterface {
    boolean includeDigits();
    boolean includeCaps();
    boolean includeSpecialCharacters();

    @Value.Default
    default boolean includeSmallLetters() {
        return true;
    }

    @Value.Default
    default int getMaxLength() {
        return Integer.MAX_VALUE;
    }

    @Value.Default
    default int getMinLength() {
        return 6;
    }
}
