package com.nexblocks.authguard.basic.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.config.ConfigStyle;
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
        return 30;
    }

    @Value.Default
    default int getMinLength() {
        return 6;
    }
}
