package com.authguard.service.impl;

import com.authguard.dal.model.AppDO;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.mappers.ServiceMapper;
import com.google.inject.Inject;
import com.authguard.dal.ApplicationsRepository;
import com.authguard.service.AccountsService;
import com.authguard.service.ApplicationsService;
import com.authguard.service.model.AppBO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApplicationsServiceImpl implements ApplicationsService {
    private static final String APPS_CHANNEL = "apps";

    private final ApplicationsRepository applicationsRepository;
    private final AccountsService accountsService;
    private final ServiceMapper serviceMapper;
    private final MessageBus messageBus;

    @Inject
    public ApplicationsServiceImpl(final ApplicationsRepository applicationsRepository,
                                   final AccountsService accountsService,
                                   final ServiceMapper serviceMapper,
                                   final MessageBus messageBus) {
        this.applicationsRepository = applicationsRepository;
        this.accountsService = accountsService;
        this.serviceMapper = serviceMapper;
        this.messageBus = messageBus;
    }

    @Override
    public AppBO create(final AppBO app) {
        /*
         * It's undecided whether an app should be under an
         * account or not. So for now, we only check that the
         * account exists if it's set.
         */
        if (app.getParentAccountId() != null && accountsService.getById(app.getParentAccountId()).isEmpty()) {
            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + app.getParentAccountId() + " exists");
        }

        final AppDO appDO = serviceMapper.toDO(app.withId(UUID.randomUUID().toString()));

        return applicationsRepository.save(appDO)
                .thenApply(serviceMapper::toBO)
                .thenApply(created -> {
                    messageBus.publish(APPS_CHANNEL, Messages.created(created));
                    return created;
                })
                .join();
    }

    @Override
    public Optional<AppBO> getById(final String id) {
        return applicationsRepository.getById(id)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<AppBO> getByExternalId(final String externalId) {
        return applicationsRepository.getById(externalId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<AppBO> update(final AppBO app) {
        final AppDO appDO = serviceMapper.toDO(app);

        return applicationsRepository.update(appDO)
                .thenApply(optional -> optional
                        .map(serviceMapper::toBO)
                        .map(updated -> {
                            messageBus.publish(APPS_CHANNEL, Messages.updated(updated));
                            return updated;
                        })
                )
                .join();
    }

    @Override
    public Optional<AppBO> delete(final String id) {
        return applicationsRepository.delete(id)
                .thenApply(optional -> optional
                        .map(serviceMapper::toBO)
                        .map(deleted -> {
                            messageBus.publish(APPS_CHANNEL, Messages.deleted(deleted));
                            return deleted;
                        })
                )
                .join();
    }

    @Override
    public Optional<AppBO> activate(final String id) {
        return getById(id)
                .map(app -> app.withActive(true))
                .flatMap(this::update);
    }

    @Override
    public Optional<AppBO> deactivate(final String id) {
        return getById(id)
                .map(app -> app.withActive(false))
                .flatMap(this::update);
    }

    @Override
    public List<AppBO> getByAccountId(final String accountId) {
        return applicationsRepository.getAllForAccount(accountId)
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()))
                .join();
    }
}
