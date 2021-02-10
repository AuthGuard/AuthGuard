package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Entity;

import java.util.Optional;

public interface CrudService<T extends Entity> {
    T create(T entity);

    Optional<T> getById(String id);

    Optional<T> update(T entity);

    Optional<T> delete(String id);
}
