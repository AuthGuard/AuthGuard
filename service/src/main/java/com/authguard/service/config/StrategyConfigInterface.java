package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = StrategyConfig.class)
public interface StrategyConfigInterface {
    String getTokenLife();
    String getRefreshTokenLife();

    // this stupid way to name getters is there only because of the way Lightbend config works
    boolean getUseJti();
    boolean getIncludePermissions();
    boolean getIncludeRoles();
    boolean getIncludeScopes();
    boolean getIncludeExternalId();
}
