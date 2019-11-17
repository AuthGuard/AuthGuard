package org.auther.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true
)
@JsonDeserialize(as = ImmutableStrategiesConfig.class)
public interface StrategiesConfig {
    ImmutableStrategyConfig getIdToken();
    ImmutableStrategyConfig getAccessToken();
}
