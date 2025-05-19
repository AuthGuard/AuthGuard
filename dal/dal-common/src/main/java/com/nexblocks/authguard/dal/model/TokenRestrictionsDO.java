package com.nexblocks.authguard.dal.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import java.util.Set;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRestrictionsDO {
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> permissions;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> scopes;
}
