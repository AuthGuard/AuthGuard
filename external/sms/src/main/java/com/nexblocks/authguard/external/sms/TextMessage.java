package com.nexblocks.authguard.external.sms;

import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@Value.Style(validationMethod = Value.Style.ValidationMethod.NONE)
public interface TextMessage {
    String getTo();
    String getTemplate();
    Map<String, Object> getParameters();
}
