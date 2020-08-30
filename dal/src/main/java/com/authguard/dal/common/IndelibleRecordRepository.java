package com.authguard.dal.common;

import com.authguard.dal.exceptions.IllegalOperationException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IndelibleRecordRepository<T> extends Repository<T> {
    @Override
    default CompletableFuture<Optional<T>> delete(final String id) {
        return CompletableFuture.failedFuture(new IllegalOperationException());
    }
}
