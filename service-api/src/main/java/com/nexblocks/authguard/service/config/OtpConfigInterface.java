package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = OtpConfig.class)
public interface OtpConfigInterface {
    OtpMode getMode();
    int getLength();
    String getLifeTime();
    String getGenerateToken();
}
