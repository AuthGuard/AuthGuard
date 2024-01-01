package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Entity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CrudService<T extends Entity> {
    CompletableFuture<T> create(T entity);

    CompletableFuture<Optional<T>> getById(long id);

    CompletableFuture<Optional<T>> update(T entity);

    CompletableFuture<Optional<T>> delete(long id);
}
