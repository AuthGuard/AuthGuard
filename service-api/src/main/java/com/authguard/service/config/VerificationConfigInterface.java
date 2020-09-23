package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = VerificationConfig.class)
public interface VerificationConfigInterface {
    String getVerifyEmailUrlTemplate();
    String getEmailVerificationLife();
}
