package com.authguard.service;

import com.authguard.service.model.Entity;
import com.authguard.service.model.RequestContextBO;

public interface IdempotentCrudService<T extends Entity> extends CrudService<T> {
    T create(T entity, RequestContextBO requestContext);

    @Override
    default T create(T entity) {
        throw new UnsupportedOperationException();
    }
}
