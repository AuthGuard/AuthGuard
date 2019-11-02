package org.auther.service.impl.jwt.config;

import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(
        create = "new",
        beanFriendlyModifiables = true,
        validationMethod = Value.Style.ValidationMethod.NONE
)
public interface StrategiesConfig {
    ModifiableStrategyConfig getRegular();
    ModifiableStrategyConfig getIdToken();
    ModifiableStrategyConfig getAccessToken();
}
