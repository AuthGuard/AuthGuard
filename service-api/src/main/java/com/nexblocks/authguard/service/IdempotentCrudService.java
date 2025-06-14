package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Entity;
import com.nexblocks.authguard.service.model.RequestContextBO;

import io.smallrye.mutiny.Uni;

public interface IdempotentCrudService<T extends Entity> extends CrudService<T> {
    Uni<T> create(T entity, RequestContextBO requestContext);

    @Override
    default Uni<T> create(T entity) {
        throw new UnsupportedOperationException();
    }
}
