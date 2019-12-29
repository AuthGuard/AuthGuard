package org.auther.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AppDTO.class)
@JsonDeserialize(as = AppDTO.class)
public interface App {
    String getId();
    String getName();
    String getAccountId();
    String isActive();
}
