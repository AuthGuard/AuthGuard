package org.auther.dal.mock;

import org.auther.dal.model.AbstractDO;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AbstractRepository<T extends AbstractDO> {
    private final Map<String, T> repo;

    public AbstractRepository() {
        repo = new HashMap<>();
    }

    public T save(final T record) {
        repo.putIfAbsent(record.getId(), record);
        return record;
    }

    public Optional<T> getById(final String id) {
        return Optional.ofNullable(repo.get(id));
    }

    public Collection<T> getAll() {
        return repo.values();
    }

    public Optional<T> update(final T record) {
        if (!repo.containsKey(record.getId())) {
            return Optional.empty();
        }

        return Optional.of(save(record));
    }

    public Optional<T> delete(final String id) {
        return Optional.ofNullable(repo.remove(id));
    }

    protected Map<String, T> getRepo() {
        return repo;
    }
}