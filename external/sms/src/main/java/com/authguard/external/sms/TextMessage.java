package com.authguard.external.sms;

import org.immutables.value.Value;

@Value.Immutable
public interface TextMessage {
    String getTo();
    String getBody();
}
