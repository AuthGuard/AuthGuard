package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = PasswordlessConfig.class)
public interface PasswordlessConfigInterface {
    String getGenerateToken();
    String getTokenLife();
    Integer getRandomSize();
}
