package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface ExchangeAttempt extends Entity {
     String getEntityId();
     String getExchangeFrom();
     String getExchangeTo();
     boolean isSuccessful();

     @Override
     @Value.Derived
     default String getEntityType() {
          return "ExchangeAttempt";
     }
}
