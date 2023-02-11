package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Duration;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = DurationRequestDTO.class)
@JsonDeserialize(as = DurationRequestDTO.class)
public interface DurationRequest {
    int getDays();
    int getHours();
    int getMinutes();

    @JsonIgnore
    default Duration toDuration() {
        return Duration.ofDays(getDays())
                .plusHours(getHours())
                .plusMinutes(getMinutes());
    }
}
