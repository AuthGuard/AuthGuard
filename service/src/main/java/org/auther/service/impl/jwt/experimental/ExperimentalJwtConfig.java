package org.auther.service.impl.jwt.experimental;


import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Modifiable
@Value.Style(
        create = "new",
        beanFriendlyModifiables = true,
        validationMethod = Value.Style.ValidationMethod.NONE
)
public interface ExperimentalJwtConfig {
    String getAlgorithm();
    String getKey();
    String getIssuer();
    String getTokenLife();
    String getRefreshTokenLife();
    List<String> getAllowedAlgorithms();
    List<String> getTrustedIssuers();
}
