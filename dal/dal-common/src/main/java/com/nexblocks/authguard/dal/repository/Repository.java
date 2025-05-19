package com.nexblocks.authguard.dal.repository;

import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface Repository<T> {
    Uni<Optional<T>> getById(long id);
    Uni<T> save(T entity);
    Uni<Optional<T>> update(T entity);
    Uni<Optional<T>> delete(long id);
}
