package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = CryptoKeyDTO.class)
@JsonDeserialize(as = CryptoKeyDTO.class)
public interface CryptoKey {
    String getId();
    String getDomain();
    Long getAccountId();
    Long getAppId();
    Instant getCreatedAt();
    String getName();
    String getAlgorithm();
    int getSize();
    String getPrivateKey();
    String getPublicKey();
}
