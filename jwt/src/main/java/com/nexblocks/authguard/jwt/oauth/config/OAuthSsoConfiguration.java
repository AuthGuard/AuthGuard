package com.nexblocks.authguard.jwt.oauth.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.Set;

@Value.Immutable
@Value.Style(
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true
)
@JsonSerialize(as = ImmutableOAuthSsoConfiguration.class)
@JsonDeserialize(as = ImmutableOAuthSsoConfiguration.class)
public interface OAuthSsoConfiguration {
    boolean useUsername();
    boolean useEmail();
    boolean usePhoneNumber();

    @Value.Default
    default String getLoginPage() {
        return "resources/sso-login.html";
    }

    @Value.Default
    default String getOtpPage() {
        return "resources/sso-otp.html";
    }

    @Value.Default
    default Set<String> getDomains() {
        return Collections.emptySet();
    }
}
