package com.nexblocks.authguard.service.model;

import java.time.Instant;

public interface Entity {
    String getId();
    Instant getCreatedAt();
    Instant getLastModified();
    String getEntityType();
}
