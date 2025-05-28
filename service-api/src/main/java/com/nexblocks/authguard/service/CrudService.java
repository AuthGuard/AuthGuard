package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Entity;

import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface CrudService<T extends Entity> {
    Uni<T> create(T entity);

    Uni<Optional<T>> getById(long id, String domain);

    Uni<Optional<T>> update(T entity, String domain);

    Uni<Optional<T>> delete(long id, String domain);
}
