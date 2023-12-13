package com.nexblocks.authguard.service.exceptions;

import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.EntityType;

public class ServiceAuthorizationException extends ServiceException {
    private final EntityType entityType;
    private final Long entityId;

    public ServiceAuthorizationException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
        this.entityType = null;
        this.entityId = null;
    }

    public ServiceAuthorizationException(final ErrorCode errorCode, final String message,
                                         final EntityType entityType, final long entityId) {
        super(errorCode, message);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }
}
