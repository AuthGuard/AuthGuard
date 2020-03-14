package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.VerificationService;
import com.authguard.service.config.ImmutableAccountConfig;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.impl.mappers.ServiceMapper;
import com.google.inject.Inject;
import com.authguard.dal.AccountsRepository;
import com.authguard.service.AccountsService;
import com.authguard.service.PermissionsService;
import com.authguard.service.RolesService;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.PermissionBO;
import com.google.inject.name.Named;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccountsServiceImpl implements AccountsService {
    private final AccountsRepository accountsRepository;
    private final PermissionsService permissionsService;
    private final ImmutableAccountConfig accountConfig;
    private final VerificationService verificationService;
    private final ServiceMapper serviceMapper;

    private final PermissionsAggregator permissionsAggregator;

    @Inject
    public AccountsServiceImpl(final AccountsRepository accountsRepository,
                               final PermissionsService permissionsService,
                               final VerificationService verificationService,
                               final RolesService rolesService,
                               final ServiceMapper serviceMapper,
                               final @Named("account") ConfigContext accountConfigContext) {
        this.accountsRepository = accountsRepository;
        this.permissionsService = permissionsService;
        this.verificationService = verificationService;
        this.serviceMapper = serviceMapper;
        this.accountConfig = accountConfigContext.asConfigBean(ImmutableAccountConfig.class);

        this.permissionsAggregator = new PermissionsAggregator(rolesService, permissionsService);
    }

    @Override
    public AccountBO create(final AccountBO account) {
        return Optional.of(account)
                .map(accountBO -> accountBO
                        .withId(UUID.randomUUID().toString())
                )
                .map(serviceMapper::toDO)
                .map(accountsRepository::save)
                .map(serviceMapper::toBO)
                .map(created -> {
                    if (accountConfig.isVerifyEmail()) {
                        verificationService.sendVerificationEmail(account);
                    }

                    return created;
                })
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Optional<AccountBO> getById(final String accountId) {
        return accountsRepository.getById(accountId)
                .map(serviceMapper::toBO);
    }

    @Override
    public List<PermissionBO> getPermissions(final String accountId) {
        final AccountBO account = getById(accountId)
                .orElseThrow(ServiceNotFoundException::new);

        return permissionsAggregator.aggregate(account.getRoles(), account.getPermissions());
    }

    @Override
    public AccountBO grantPermissions(final String accountId, final List<PermissionBO> permissions) {
        final List<PermissionBO> verifiedPermissions = permissionsService.validate(permissions);

        if (verifiedPermissions.size() != permissions.size()) {
            final List<PermissionBO> difference = permissions.stream()
                    .filter(permission -> !verifiedPermissions.contains(permission))
                    .collect(Collectors.toList());

            throw new ServiceException("The following permissions are not valid" + difference);
        }

        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<PermissionBO> combinedPermissions = Stream.concat(account.getPermissions().stream(), permissions.stream())
                .distinct()
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(combinedPermissions);

        accountsRepository.update(serviceMapper.toDO(updated));

        return updated;
    }

    @Override
    public AccountBO revokePermissions(final String accountId, final List<PermissionBO> permissions) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<PermissionBO> filteredPermissions = account.getPermissions().stream()
                .filter(permission -> !permissions.contains(permission))
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(filteredPermissions);

        accountsRepository.update(serviceMapper.toDO(updated));

        return updated;
    }

    @Override
    public AccountBO grantRoles(final String accountId, final List<String> roles) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<String> combinedRoles = Stream.concat(account.getRoles().stream(), roles.stream())
                .distinct()
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(combinedRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .map(serviceMapper::toBO)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public AccountBO revokeRoles(final String accountId, final List<String> roles) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<String> filteredRoles = account.getRoles().stream()
                .filter(role -> !roles.contains(role))
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(filteredRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .map(serviceMapper::toBO)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public List<AccountBO> getAdmins() {
        return accountsRepository.getAdmins()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }
}
