package com.authguard.external.email;

import org.immutables.value.Value;

@Value.Immutable
public interface Email {
    String getTo();
    String getSubject();
    String getBody();
}
