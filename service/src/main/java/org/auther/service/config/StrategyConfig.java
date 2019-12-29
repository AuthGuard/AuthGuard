package org.auther.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true,
        get = {"get*", "use*", "include*"}
)
@JsonDeserialize(as = ImmutableStrategyConfig.class)
public interface StrategyConfig {
    String getTokenLife();
    String getRefreshTokenLife();

    // this stupid way to name getters is there only because of the way Lightbend config works
    boolean getUseJti();
    boolean getIncludePermissions();
    boolean getIncludeRoles();
    boolean getIncludeScopes();
}
