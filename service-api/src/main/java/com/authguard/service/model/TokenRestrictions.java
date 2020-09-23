package com.authguard.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface TokenRestrictions {
    List<String> getPermissions();
    List<String> getScopes();
}
