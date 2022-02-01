package com.nexblocks.authguard.bindings;

import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.external.sms.ImmutableTextMessage;
import com.nexblocks.authguard.external.sms.SmsProvider;

public class PlaceholderProviderSink implements EmailProvider, SmsProvider {
    @Override
    public void send(final ImmutableEmail email) {
    }

    @Override
    public void send(final ImmutableTextMessage message) {
    }
}
