package com.authguard.service.impl;

import com.authguard.dal.model.AbstractDO;
import com.authguard.dal.repository.Repository;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.model.Entity;
import com.authguard.service.util.ID;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;

public class PersistenceService<BO extends Entity, DO extends AbstractDO, R extends Repository<DO>> {
    private final R repository;
    private final MessageBus messageBus;
    private final Function<BO, DO> boToDo;
    private final Function<DO, BO> doToBo;
    private final String channel;

    public PersistenceService(final R repository,
                              final MessageBus messageBus,
                              final Function<BO, DO> boToDo,
                              final Function<DO, BO> doToBo,
                              final String channel) {
        this.repository = repository;
        this.messageBus = messageBus;
        this.boToDo = boToDo;
        this.doToBo = doToBo;
        this.channel = channel;
    }

    public BO create(final BO entity) {
        final OffsetDateTime now = OffsetDateTime.now();
        final DO mappedDo = boToDo.apply(entity);

        mappedDo.setId(ID.generate());
        mappedDo.setDeleted(false);
        mappedDo.setCreatedAt(now);
        mappedDo.setLastModified(now);

        return repository.save(mappedDo)
                .thenApply(persisted -> {
                    final BO persistedBo = doToBo.apply(persisted);

                    messageBus.publish(channel, Messages.created(persistedBo));

                    return persistedBo;
                })
                .join();
    }

    public Optional<BO> getById(final String id) {
        return repository.getById(id)
                .thenApply(opt -> opt.map(doToBo))
                .join();
    }

    public Optional<BO> update(final BO entity) {
        final OffsetDateTime now = OffsetDateTime.now();
        final DO mappedDo = boToDo.apply(entity);

        mappedDo.setLastModified(now);

        return repository.update(mappedDo)
                .thenApply(opt -> {
                    final Optional<BO> boOpt = opt.map(doToBo);

                    boOpt.ifPresent(bo -> messageBus.publish(channel, Messages.updated(bo)));

                    return boOpt;
                })
                .join();
    }

    public Optional<BO> delete(final String id) {
        return repository.delete(id)
                .thenApply(opt -> {
                    final Optional<BO> boOpt = opt.map(doToBo);

                    boOpt.ifPresent(bo -> messageBus.publish(channel, Messages.deleted(bo)));

                    return boOpt;
                })
                .join();
    }
}
