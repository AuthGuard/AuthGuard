package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface AccountSession {
    AccountBO getAccount();
    Session getSession();
}
