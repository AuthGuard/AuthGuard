package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = ImmutableAuthenticationConfig.class)
public interface AuthenticationConfig {
    String getGenerateToken();
    boolean getUseOtp();
}
