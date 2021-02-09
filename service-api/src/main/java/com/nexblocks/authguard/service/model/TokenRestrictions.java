package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
@BOStyle
public interface TokenRestrictions {
    Set<String> getPermissions();
    Set<String> getScopes();
}
