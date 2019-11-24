package org.auther.service.impl;

import com.google.inject.Inject;
import org.auther.dal.AccountsRepository;
import org.auther.service.AccountsService;
import org.auther.service.PermissionsService;
import org.auther.service.RolesService;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.exceptions.ServiceNotFoundException;
import org.auther.service.impl.mappers.ServiceMapper;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccountsServiceImpl implements AccountsService {
    private final AccountsRepository accountsRepository;
    private final PermissionsService permissionsService;
    private final ServiceMapper serviceMapper;

    private final PermissionsAggregator permissionsAggregator;

    @Inject
    public AccountsServiceImpl(final AccountsRepository accountsRepository, final PermissionsService permissionsService,
                               final RolesService rolesService, final ServiceMapper serviceMapper) {
        this.accountsRepository = accountsRepository;
        this.permissionsService = permissionsService;
        this.serviceMapper = serviceMapper;

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
}
