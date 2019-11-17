package org.auther.service.config;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true
)
@JsonDeserialize(as = ImmutableJwtConfig.class)
public interface JwtConfig {
    String getAlgorithm();
    String getKey();
    String getIssuer();
    List<String> getAllowedAlgorithms();
    List<String> getTrustedIssuers();
}
