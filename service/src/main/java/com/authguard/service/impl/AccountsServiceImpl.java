package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.dal.model.AccountDO;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.VerificationMessageService;
import com.authguard.service.config.AccountConfig;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AccountEmailBO;
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
    private static final String ACCOUNTS_CHANNEL = "accounts";

    private final AccountsRepository accountsRepository;
    private final PermissionsService permissionsService;
    private final AccountConfig accountConfig;
    private final VerificationMessageService verificationMessageService;
    private final ServiceMapper serviceMapper;
    private final MessageBus messageBus;

    private final PermissionsAggregator permissionsAggregator;

    @Inject
    public AccountsServiceImpl(final AccountsRepository accountsRepository,
                               final PermissionsService permissionsService,
                               final VerificationMessageService verificationMessageService,
                               final RolesService rolesService,
                               final ServiceMapper serviceMapper,
                               final MessageBus messageBus,
                               final @Named("account") ConfigContext accountConfigContext) {
        this.accountsRepository = accountsRepository;
        this.permissionsService = permissionsService;
        this.verificationMessageService = verificationMessageService;
        this.serviceMapper = serviceMapper;
        this.messageBus = messageBus;
        this.accountConfig = accountConfigContext.asConfigBean(AccountConfig.class);

        this.permissionsAggregator = new PermissionsAggregator(rolesService, permissionsService);
    }

    @Override
    public AccountBO create(final AccountBO account) {
        final AccountDO accountDO = serviceMapper.toDO(account.withId(UUID.randomUUID().toString()));

        return accountsRepository.save(accountDO)
                .thenApply(serviceMapper::toBO)
                .thenApply(created -> {
                    messageBus.publish(ACCOUNTS_CHANNEL, Messages.created(created));

                    if (accountConfig.verifyEmail()) {
                        verificationMessageService.sendVerificationEmail(created);
                    }

                    return created;
                }).join();
    }

    @Override
    public Optional<AccountBO> getById(final String accountId) {
        return accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO);
    }

    @Override
    public Optional<AccountBO> update(final AccountBO account) {
        return accountsRepository.update(serviceMapper.toDO(account))
                .join()
                .map(serviceMapper::toBO)
                .map(updated -> {
                    messageBus.publish(ACCOUNTS_CHANNEL, Messages.updated(updated));
                    return updated;
                });
    }

    @Override
    public Optional<AccountBO> removeEmails(final String accountId, final List<String> emails) {
        final AccountBO existing = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<AccountEmailBO> newEmails = existing.getEmails().stream()
                .filter(email -> !emails.contains(email.getEmail()))
                .collect(Collectors.toList());

        final AccountBO updated = AccountBO.builder().from(existing)
                .emails(newEmails)
                .build();

        return update(updated);
    }

    @Override
    public Optional<AccountBO> addEmails(final String accountId, final List<AccountEmailBO> emails) {
        final AccountBO existing = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<String> currentEmails = existing.getEmails().stream()
                .map(AccountEmailBO::getEmail)
                .collect(Collectors.toList());

        final List<AccountEmailBO> newEmails = emails.stream()
                .filter(email -> !currentEmails.contains(email.getEmail()))
                .collect(Collectors.toList());

        final AccountBO updated = AccountBO.builder().from(existing)
                .addAllEmails(newEmails)
                .build();

        return update(updated);
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
                .join()
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
                .join()
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
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<String> combinedRoles = Stream.concat(account.getRoles().stream(), roles.stream())
                .distinct()
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(combinedRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public AccountBO revokeRoles(final String accountId, final List<String> roles) {
        final AccountBO account = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<String> filteredRoles = account.getRoles().stream()
                .filter(role -> !roles.contains(role))
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(filteredRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public List<AccountBO> getAdmins() {
        return accountsRepository.getAdmins()
                .join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }
}
