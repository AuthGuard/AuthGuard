package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface ExchangeAttempt extends Entity {
     Long getEntityId();
     String getExchangeFrom();
     String getExchangeTo();
     boolean isSuccessful();
     String getDeviceId();
     String getClientId();
     String getExternalSessionId();
     String getSourceIp();
     String getUserAgent();

     @Override
     @Value.Derived
     default String getEntityType() {
          return "ExchangeAttempt";
     }
}
