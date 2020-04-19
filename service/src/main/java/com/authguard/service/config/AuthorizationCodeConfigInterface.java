package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = AuthorizationCodeConfig.class)
public interface AuthorizationCodeConfigInterface {
    Integer getRandomSize();
    String getLifeTime();
}
