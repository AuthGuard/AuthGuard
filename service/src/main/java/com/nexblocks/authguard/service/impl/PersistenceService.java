package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.AbstractDO;
import com.nexblocks.authguard.dal.repository.Repository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.model.Entity;
import com.nexblocks.authguard.service.util.ID;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
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

    public Uni<BO> create(final BO entity) {
        final Instant now = Instant.now();
        final DO mappedDo = boToDo.apply(entity);

        mappedDo.setId(ID.generate());
        mappedDo.setDeleted(false);
        mappedDo.setCreatedAt(now);
        mappedDo.setLastModified(now);

        return repository.save(mappedDo)
                .map(persisted -> {
                    final BO persistedBo = doToBo.apply(persisted);

                    if (channel != null) {
                        messageBus.publish(channel, Messages.created(persistedBo, entity.getDomain()));
                    }

                    return persistedBo;
                });
    }

    public Uni<Optional<BO>> getById(final long id) {
        return repository.getById(id)
                .map(opt -> opt.map(doToBo));
    }

    public Uni<Optional<BO>> getById(final long id, final String domain) {
        return repository.getById(id)
                .map(opt -> opt.map(doToBo)
                        .filter(bo -> Objects.equals(bo.getDomain(), domain)));
    }

    public Uni<Optional<BO>> update(final BO entity) {
        final Instant now = Instant.now();
        final DO mappedDo = boToDo.apply(entity);

        mappedDo.setLastModified(now);

        return repository.update(mappedDo)
                .map(opt -> {
                    final Optional<BO> boOpt = opt.map(doToBo);

                    if (channel != null) {
                        boOpt.ifPresent(bo -> messageBus.publish(channel, Messages.updated(bo, entity.getDomain())));
                    }

                    return boOpt;
                });
    }

    public Uni<Optional<BO>> delete(final long id) {
        return repository.delete(id)
                .map(opt -> {
                    final Optional<BO> boOpt = opt.map(doToBo);

                    if (channel != null) {
                        boOpt.ifPresent(bo -> messageBus.publish(channel, Messages.deleted(bo, bo.getDomain())));
                    }

                    return boOpt;
                });
    }
}
