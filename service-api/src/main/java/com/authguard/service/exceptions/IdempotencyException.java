package com.authguard.service.exceptions;

import com.authguard.service.model.IdempotentRecordBO;

public class IdempotencyException extends RuntimeException {
    private final IdempotentRecordBO idempotentRecord;

    public IdempotencyException(final IdempotentRecordBO idempotentRecord) {
        this.idempotentRecord = idempotentRecord;
    }

    public IdempotentRecordBO getIdempotentRecord() {
        return idempotentRecord;
    }
}
