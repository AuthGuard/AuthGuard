package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.AppDO;
import com.nexblocks.authguard.dal.persistence.ApplicationsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationsServiceImpl implements ApplicationsService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationsServiceImpl.class);

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

        return persistenceService.create(app);
    }

    @Override
    public Optional<AppBO> getById(final long id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<AppBO> getByExternalId(final long externalId) {
        return applicationsRepository.getById(externalId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<AppBO> update(final AppBO app) {
        LOG.info("Application update request. accountId={}", app.getId());

        // FIXME accountId cannot be updated
        return persistenceService.update(app);
    }

    @Override
    public Optional<AppBO> delete(final long id) {
        LOG.info("Application delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public Optional<AppBO> activate(final long id) {
        return getById(id)
                .flatMap(app -> {
                    LOG.info("Activate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO activated = app.withActive(true);
                    Optional<AppBO> persisted = this.update(activated);

                    if (persisted.isPresent()) {
                        LOG.info("Application activated. appId={}, domain={}", app.getId(), app.getDomain());
                    } else {
                        LOG.info("Failed to activate application. appId={}, domain={}", app.getId(), app.getDomain());
                    }

                    return persisted;
                });
    }

    @Override
    public Optional<AppBO> deactivate(final long id) {
        return getById(id)
                .flatMap(app -> {
                    LOG.info("Deactivate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO deactivated = app.withActive(false);
                    Optional<AppBO> persisted = this.update(deactivated);

                    if (persisted.isPresent()) {
                        LOG.info("Application deactivated. appId={}, domain={}", app.getId(), app.getDomain());
                    } else {
                        LOG.info("Failed to deactivate application. appId={}, domain={}", app.getId(), app.getDomain());
                    }

                    return persisted;
                });
    }

    @Override
    public List<AppBO> getByAccountId(final long accountId) {
        return applicationsRepository.getAllForAccount(accountId)
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()))
                .join();
    }
}
