package com.nexblocks.authguard.basic.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.config.ConfigStyle;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = OtpConfig.class)
public interface OtpConfigInterface {
    OtpMode getMode();
    int getLength();
    String getLifeTime();
    String getGenerateToken();
    Method getMethod();

    enum Method {
        EMAIL,
        SMS
    }
}
