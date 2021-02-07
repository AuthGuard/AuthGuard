package com.authguard.service.impl;

import com.authguard.dal.model.AppDO;
import com.authguard.dal.persistence.ApplicationsRepository;
import com.authguard.emb.MessageBus;
import com.authguard.service.AccountsService;
import com.authguard.service.ApplicationsService;
import com.authguard.service.IdempotencyService;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.RequestContextBO;
import com.authguard.service.util.ID;
import com.google.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationsServiceImpl implements ApplicationsService {
    private static final String APPS_CHANNEL = "apps";

    private final ApplicationsRepository applicationsRepository;
    private final AccountsService accountsService;
    private final IdempotencyService idempotencyService;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<AppBO, AppDO, ApplicationsRepository> persistenceService;

    @Inject
    public ApplicationsServiceImpl(final ApplicationsRepository applicationsRepository,
                                   final AccountsService accountsService,
                                   final IdempotencyService idempotencyService,
                                   final ServiceMapper serviceMapper,
                                   final MessageBus messageBus) {
        this.applicationsRepository = applicationsRepository;
        this.accountsService = accountsService;
        this.idempotencyService = idempotencyService;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(applicationsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, APPS_CHANNEL);
    }

    @Override
    public AppBO create(final AppBO app, final RequestContextBO requestContext) {
        return idempotencyService.performOperation(() -> doCreate(app), requestContext.getIdempotentKey(), app.getEntityType())
                .join();
    }

    private AppBO doCreate(final AppBO app) {
        /*
         * It's undecided whether an app should be under an
         * account or not. So for now, we only check that the
         * account exists if it's set.
         */
        if (app.getParentAccountId() != null && accountsService.getById(app.getParentAccountId()).isEmpty()) {
            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + app.getParentAccountId() + " exists");
        }

        return persistenceService.create(app.withId(ID.generate()));
    }

    @Override
    public Optional<AppBO> getById(final String id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<AppBO> getByExternalId(final String externalId) {
        return applicationsRepository.getById(externalId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<AppBO> update(final AppBO app) {
        // FIXME accountId cannot be updated
        return persistenceService.update(app);
    }

    @Override
    public Optional<AppBO> delete(final String id) {
        return persistenceService.delete(id);
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
