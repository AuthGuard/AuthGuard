package org.auther.service.config;


import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Modifiable
@Value.Style(
        create = "new",
        beanFriendlyModifiables = true,
        validationMethod = Value.Style.ValidationMethod.NONE
)
public interface JwtConfig {
    String getAlgorithm();
    String getKey();
    String getIssuer();
    List<String> getAllowedAlgorithms();
    List<String> getTrustedIssuers();
}
