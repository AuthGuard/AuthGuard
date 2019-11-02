package org.auther.service.impl.jwt.config;

import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(
        create = "new",
        beanFriendlyModifiables = true,
        validationMethod = Value.Style.ValidationMethod.NONE
)
public interface StrategyConfig {
    String getTokenLife();
    String getRefreshTokenLife();

    // this stupid way to name getters is there only because of the way Lightbend config works
    boolean getUseJti();
    boolean getIncludePermissions();
    boolean getIncludeRoles();
    boolean getIncludeScopes();
    boolean getSignedRefreshTokens();
}
