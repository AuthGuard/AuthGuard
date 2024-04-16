package com.nexblocks.authguard.service.model;

import java.time.Instant;

public interface Entity {
    long getId();
    String getDomain();
    Instant getCreatedAt();
    Instant getLastModified();
    String getEntityType();
}
