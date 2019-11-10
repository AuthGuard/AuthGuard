package org.auther.service.config;

import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(
        create = "new",
        beanFriendlyModifiables = true,
        validationMethod = Value.Style.ValidationMethod.NONE
)
public interface StrategiesConfig {
    ModifiableStrategyConfig getIdToken();
    ModifiableStrategyConfig getAccessToken();
}
