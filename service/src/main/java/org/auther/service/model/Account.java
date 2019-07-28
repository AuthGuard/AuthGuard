package org.auther.service.model;

import org.jetbrains.annotations.Nullable;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface Account {
    String getId();
    String getUsername();
    @Nullable String getPassword();
    List<String> permissions();
}
