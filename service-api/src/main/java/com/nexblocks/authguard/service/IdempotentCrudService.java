package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Entity;
import com.nexblocks.authguard.service.model.RequestContextBO;

import java.util.concurrent.CompletableFuture;

public interface IdempotentCrudService<T extends Entity> extends CrudService<T> {
    CompletableFuture<T> create(T entity, RequestContextBO requestContext);

    @Override
    default CompletableFuture<T> create(T entity) {
        throw new UnsupportedOperationException();
    }
}
