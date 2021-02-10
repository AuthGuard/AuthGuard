package com.nexblocks.authguard.service.model;

import java.time.OffsetDateTime;

public interface Entity {
    String getId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getLastModified();
    String getEntityType();
}
