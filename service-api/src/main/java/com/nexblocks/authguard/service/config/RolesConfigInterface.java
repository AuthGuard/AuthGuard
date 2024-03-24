package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = RolesConfig.class)
public interface RolesConfigInterface {
    String getName();
    boolean isForAccounts();
    boolean isForApplications();
}
