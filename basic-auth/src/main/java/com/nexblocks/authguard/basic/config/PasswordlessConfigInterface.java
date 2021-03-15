package com.nexblocks.authguard.basic.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.config.ConfigStyle;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = PasswordlessConfig.class)
public interface PasswordlessConfigInterface {
    String getGenerateToken();
    String getTokenLife();
    Integer getRandomSize();
}
