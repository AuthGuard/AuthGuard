package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.model.UserIdentifier;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = TotpAuthenticatorsConfig.class)
public interface TotpAuthenticatorsConfigInterface {
    @Value.Default
    default Integer getVersion() {
        return 1;
    }

    boolean generateQrCode();
    String getQrIssuer();
    UserIdentifier.Type getQrUserIdentifierType();

    String getEncryptionKey();

    List<AuthenticatorConfig> getCustomAuthenticators();

    @Value.Immutable
    @ConfigStyle
    @JsonDeserialize(as = AuthenticatorConfig.class)
    interface AuthenticatorConfigInterface {
        String getName();
        int getTimeStep();
    }
}
