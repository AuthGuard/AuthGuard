package com.nexblocks.authguard.dal.repository;

import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface ImmutableRecordRepository<T> extends Repository<T> {
    @Override
    default Uni<Optional<T>> update(T record) {
        return Uni.createFrom().failure(new UnsupportedOperationException());
    }
}
