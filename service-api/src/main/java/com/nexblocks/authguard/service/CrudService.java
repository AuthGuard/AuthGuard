package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Entity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CrudService<T extends Entity> {
    CompletableFuture<T> create(T entity);

    CompletableFuture<Optional<T>> getById(long id, String domain);

    CompletableFuture<Optional<T>> update(T entity, String domain);

    CompletableFuture<Optional<T>> delete(long id, String domain);
}
