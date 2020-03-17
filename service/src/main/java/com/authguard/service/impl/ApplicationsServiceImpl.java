package com.authguard.service.impl;

import com.authguard.dal.model.AppDO;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.impl.mappers.ServiceMapper;
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
        final AppDO appDO = serviceMapper.toDO(app.withId(UUID.randomUUID().toString()));

        return applicationsRepository.save(appDO)
                .thenApply(serviceMapper::toBO)
                .join();
    }

    @Override
    public Optional<AppBO> getById(final String id) {
        return applicationsRepository.getById(id)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<AppBO> update(final AppBO app) {
        final AppDO appDO = serviceMapper.toDO(app);

        return applicationsRepository.update(appDO)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<AppBO> delete(final String id) {
        return applicationsRepository.delete(id)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public List<AppBO> getByAccountId(final String accountId) {
        return applicationsRepository.getAllForAccount(accountId)
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()))
                .join();
    }
}
