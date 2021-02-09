package com.nexblocks.authguard.external.sms;

public interface SmsProvider {
    void send(ImmutableTextMessage message);
}
