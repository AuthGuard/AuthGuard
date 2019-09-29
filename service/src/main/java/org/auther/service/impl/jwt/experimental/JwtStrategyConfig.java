package org.auther.service.impl.jwt.experimental;

import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(
        create = "new",
        beanFriendlyModifiables = true,
        get = { "use*", "include*" }
)
public interface JwtStrategyConfig {
    String useJti();
    String includePermissions();
    String includeRoles();
    String includeScopes();
}
