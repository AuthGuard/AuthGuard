package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = CryptoKeyRequestDTO.class)
@JsonDeserialize(as = CryptoKeyRequestDTO.class)
public interface CryptoKeyRequest {
    Algorithm getAlgorithm();
    Integer getSize();
    String getName();
    Long getAccountId();
    Long getAppId();
    boolean isPersist();
    boolean isPasscodeProtected();
    String getPasscode();

    enum Algorithm {
        RSA,
        AES,
        EC_SECP128K1
    }
}
