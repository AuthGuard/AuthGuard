package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = TotpKeyDTO.class)
@JsonSerialize(as = TotpKeyDTO.class)
public interface TotpKey extends DomainScoped {
    long getAccountId();
    String getAuthenticator();
    String getKey();
    String getQrCode();
}
