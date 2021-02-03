package com.authguard.dal.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IndelibleRecordRepository<T> extends Repository<T> {
    @Override
    default CompletableFuture<Optional<T>> delete(final String id) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }
}
