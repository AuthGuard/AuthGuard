package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = StrategyConfig.class)
public interface StrategyConfigInterface {
    String getTokenLife();
    String getRefreshTokenLife();
    boolean useJti();
    boolean includePermissions();
    boolean includeRoles();
    boolean includeExternalId();
}
