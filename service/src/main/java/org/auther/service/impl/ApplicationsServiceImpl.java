package org.auther.service.impl;

import com.google.inject.Inject;
import org.auther.dal.ApplicationsRepository;
import org.auther.service.AccountsService;
import org.auther.service.ApplicationsService;
import org.auther.service.exceptions.ServiceNotFoundException;
import org.auther.service.impl.mappers.ServiceMapper;
import org.auther.service.model.AppBO;

import java.util.Optional;
import java.util.UUID;

public class ApplicationsServiceImpl implements ApplicationsService {
    private final ApplicationsRepository applicationsRepository;
    private final AccountsService accountsService;
    private final ServiceMapper serviceMapper;

    @Inject
    public ApplicationsServiceImpl(final ApplicationsRepository applicationsRepository,
                                   final AccountsService accountsService,
                                   final ServiceMapper serviceMapper) {
        this.applicationsRepository = applicationsRepository;
        this.accountsService = accountsService;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public AppBO create(final AppBO app) {
        if (accountsService.getById(app.getParentAccountId()).isEmpty()) {
            throw new ServiceNotFoundException("No account with ID " + app.getParentAccountId() + " exists");
        }

        /*
         * It's undecided whether an app should be under an
         * account or not. So for now, no check is done on
         * the accountId.
         */
        return Optional.of(app)
                .map(appBO -> appBO.withId(UUID.randomUUID().toString()))
                .map(serviceMapper::toDO)
                .map(applicationsRepository::save)
                .map(serviceMapper::toBO)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Optional<AppBO> getById(final String id) {
        return applicationsRepository.getById(id)
                .map(serviceMapper::toBO);
    }

    @Override
    public Optional<AppBO> update(final AppBO app) {
        return Optional.of(app)
                .map(serviceMapper::toDO)
                .flatMap(applicationsRepository::update)
                .map(serviceMapper::toBO);
    }

    @Override
    public Optional<AppBO> delete(final String id) {
        return applicationsRepository.delete(id)
                .map(serviceMapper::toBO);
    }
}
