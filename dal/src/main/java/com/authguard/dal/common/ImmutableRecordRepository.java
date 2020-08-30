package com.authguard.dal.common;

import com.authguard.dal.exceptions.IllegalOperationException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ImmutableRecordRepository<T> extends Repository<T> {
    @Override
    default CompletableFuture<Optional<T>> update(T record) {
        return CompletableFuture.failedFuture(new IllegalOperationException());
    }
}
