package com.authguard.external.sms;

public interface SmsProvider {
    void send(ImmutableTextMessage message);
}
