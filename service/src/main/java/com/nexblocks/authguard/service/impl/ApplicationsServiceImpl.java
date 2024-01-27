package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
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
import com.nexblocks.authguard.service.util.AsyncUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
    public CompletableFuture<AppBO> create(final AppBO app, final RequestContextBO requestContext) {
        return idempotencyService.performOperationAsync(() -> doCreate(app),
                requestContext.getIdempotentKey(), app.getEntityType());
    }

    private CompletableFuture<AppBO> doCreate(final AppBO app) {
        /*
         * It's undecided whether an app should be under an
         * account or not. So for now, we only check that the
         * account exists if it's set.
         */
        if (app.getParentAccountId() != null) {
            return accountsService.getById(app.getParentAccountId(), app.getDomain())
                    .thenCompose(opt -> {
                        if (opt.isEmpty()) {
                            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + app.getParentAccountId() + " exists");
                        }

                        return persistenceService.create(app);
                    });
        }

        return persistenceService.create(app);
    }

    @Override
    public CompletableFuture<Optional<AppBO>> getById(final long id, String domain) {
        return persistenceService.getById(id)
                .thenApply(opt -> opt.filter(app -> Objects.equals(app.getDomain(), domain)));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> getByExternalId(final long externalId, String domain) {
        return applicationsRepository.getById(externalId)
                .thenApply(optional -> optional
                        .filter(app -> Objects.equals(app.getDomain(), domain))
                        .map(serviceMapper::toBO));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> update(final AppBO app, final String domain) {
        LOG.info("Application update request. accountId={}", app.getId());

        // FIXME accountId cannot be updated
        return getById(app.getId(), domain).thenCompose(ignored -> persistenceService.update(app));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> delete(final long id, final String domain) {
        LOG.info("Application delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public CompletableFuture<AppBO> activate(final long id, final String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> {
                    LOG.info("Activate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO activated = app.withActive(true);
                    return update(activated, domain)
                            .thenApply(persisted -> {
                                if (persisted.isPresent()) {
                                    LOG.info("Application activated. appId={}, domain={}", app.getId(), app.getDomain());
                                    return persisted.get();
                                }

                                LOG.info("Failed to activate application. appId={}, domain={}", app.getId(), app.getDomain());
                                throw new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Application does not exist");
                            });
                });
    }

    @Override
    public CompletableFuture<AppBO> deactivate(final long id, final String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> {
                    LOG.info("Activate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO deactivated = app.withActive(false);
                    return update(deactivated, domain)
                            .thenApply(persisted -> {
                                if (persisted.isPresent()) {
                                    LOG.info("Application deactivated. appId={}, domain={}", app.getId(), app.getDomain());
                                    return persisted.get();
                                }

                                LOG.info("Failed to deactivate application. appId={}, domain={}", app.getId(), app.getDomain());
                                throw new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Application does not exist");
                            });
                });
    }

    @Override
    public CompletableFuture<List<AppBO>> getByAccountId(final long accountId, final String domain) {
        return applicationsRepository.getAllForAccount(accountId)
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()));
    }
}
