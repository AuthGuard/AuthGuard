package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = BootstrapEntitiesConfig.class)
public interface BootstrapEntitiesConfigInterface {
    Map<String, DomainEntitiesConfig> getDomains();
}
