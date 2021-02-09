package com.nexblocks.authguard.dal.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ImmutableRecordRepository<T> extends Repository<T> {
    @Override
    default CompletableFuture<Optional<T>> update(T record) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }
}
