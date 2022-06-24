package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = DomainEntitiesConfig.class)
public interface DomainEntitiesConfigInterface {
    List<PermissionConfig> getPermissions();
    List<String> getRoles();
}
