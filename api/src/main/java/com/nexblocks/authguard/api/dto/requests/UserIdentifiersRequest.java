package com.nexblocks.authguard.api.dto.requests;

import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = UserIdentifiersRequestDTO.class)
@JsonDeserialize(as = UserIdentifiersRequestDTO.class)
public interface UserIdentifiersRequest {
    List<UserIdentifierDTO> getIdentifiers();
}
