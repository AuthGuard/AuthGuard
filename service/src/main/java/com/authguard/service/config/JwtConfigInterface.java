package com.authguard.service.config;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = JwtConfig.class)
public interface JwtConfigInterface {
    String getAlgorithm();
    String getKey();
    String getIssuer();
    List<String> getAllowedAlgorithms();
    List<String> getTrustedIssuers();
}
