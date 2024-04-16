package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Client extends Entity {
    String getExternalId();
    String getName();
    Long getAccountId();
    String getBaseUrl();
    ClientType getClientType();
    boolean isActive();
    boolean isDeleted();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "Client";
    }

    enum ClientType {
        AUTH,
        ADMIN
    }
}
