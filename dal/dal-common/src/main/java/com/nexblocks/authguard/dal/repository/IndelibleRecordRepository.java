package com.nexblocks.authguard.dal.repository;

import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface IndelibleRecordRepository<T> extends Repository<T> {
    @Override
    default Uni<Optional<T>> delete(final long id) {
        return Uni.createFrom().failure(new UnsupportedOperationException());
    }
}
