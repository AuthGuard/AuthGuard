package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Binding {
    String getPackageName();
    String getName();
    String getLocation();
}
