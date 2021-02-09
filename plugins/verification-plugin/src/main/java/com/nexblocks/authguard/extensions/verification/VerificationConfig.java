package com.nexblocks.authguard.extensions.verification;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        jdkOnly = true,
        validationMethod = Value.Style.ValidationMethod.NONE
)
@JsonDeserialize(as = ImmutableVerificationConfig.class)
public interface VerificationConfig {
    String getEmailVerificationLife();
}
