package org.auther.service.model;

import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@BOStyle
public interface HashedPassword {
    @Nullable String getPassword();
    @Nullable String getSalt();
}
