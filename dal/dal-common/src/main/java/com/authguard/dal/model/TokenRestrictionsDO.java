package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import java.util.Set;

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
