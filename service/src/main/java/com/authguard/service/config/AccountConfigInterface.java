package com.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = AccountConfig.class)
public interface AccountConfigInterface {
    boolean requireEmail();
    boolean requirePhoneNumber();
    boolean verifyEmail();
    boolean verifyPhoneNumber();
}
