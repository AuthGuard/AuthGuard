package com.authguard.api.dto.entities;

import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = CollectionResponseDTO.class)
public interface CollectionResponse<T> {
    Collection<T> getItems();
}
