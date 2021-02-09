package com.nexblocks.authguard.external.email;

public interface EmailProvider {
    void send(ImmutableEmail email);
}
