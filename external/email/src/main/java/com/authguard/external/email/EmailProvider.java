package com.authguard.external.email;

public interface EmailProvider {
    void send(ImmutableEmail email);
}
