package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = ImmutableAccountConfig.class)
public interface AccountConfig {
    boolean requireEmail();
    boolean requirePhoneNumber();
    boolean verifyEmail();
    boolean verifyPhoneNumber();
}
