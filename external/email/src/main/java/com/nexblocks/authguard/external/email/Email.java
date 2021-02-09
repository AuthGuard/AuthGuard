package com.nexblocks.authguard.external.email;

import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@Value.Style(validationMethod = Value.Style.ValidationMethod.NONE)
public interface Email {
    String getTemplate();
    String getTo();
    String getSubject();
    String getBody();
    Map<String, Object> getParameters();
}
