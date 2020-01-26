package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = ImmutableOtpConfig.class)
public interface OtpConfig {
    OtpMode getMode();
    int getLength();
    String getLifeTime();
}
