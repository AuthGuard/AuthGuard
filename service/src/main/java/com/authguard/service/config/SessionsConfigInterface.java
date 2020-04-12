package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = SessionsConfig.class)
public interface SessionsConfigInterface {
    Integer getRandomSize();
    String getLifeTime();
}
