package com.nexblocks.authguard.dal.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Repository<T> {
    CompletableFuture<Optional<T>> getById(long id);
    CompletableFuture<T> save(T entity);
    CompletableFuture<Optional<T>> update(T entity);
    CompletableFuture<Optional<T>> delete(long id);
}
