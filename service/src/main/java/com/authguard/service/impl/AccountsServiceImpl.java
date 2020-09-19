package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.dal.model.AccountDO;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.*;
import com.authguard.service.config.AccountConfig;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.RequestContextBO;
import com.google.inject.Inject;
import com.authguard.dal.AccountsRepository;
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
    private final RolesService rolesService;
    private final IdempotencyService idempotencyService;
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
                               final IdempotencyService idempotencyService,
                               final ServiceMapper serviceMapper,
                               final MessageBus messageBus,
                               final @Named("accounts") ConfigContext accountConfigContext) {
        this.accountsRepository = accountsRepository;
        this.permissionsService = permissionsService;
        this.rolesService = rolesService;
        this.verificationMessageService = verificationMessageService;
        this.idempotencyService = idempotencyService;
        this.serviceMapper = serviceMapper;
        this.messageBus = messageBus;
        this.accountConfig = accountConfigContext.asConfigBean(AccountConfig.class);

        this.permissionsAggregator = new PermissionsAggregator(rolesService, permissionsService);
    }

    @Override
    public AccountBO create(final AccountBO account, final RequestContextBO requestContext) {
        return idempotencyService
                .performOperation(() -> doCreate(account), requestContext.getIdempotentKey(), account.getEntityType())
                .join();
    }

    private AccountBO doCreate(final AccountBO account) {
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
    public Optional<AccountBO> getByExternalId(final String externalId) {
        return accountsRepository.getByExternalId(externalId)
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
    public Optional<AccountBO> delete(final String accountId) {
        return accountsRepository.delete(accountId)
                .join()
                .map(serviceMapper::toBO);
    }

    @Override
    public Optional<AccountBO> activate(final String accountId) {
        return getById(accountId)
                .map(account -> account.withActive(true))
                .flatMap(this::update);
    }

    @Override
    public Optional<AccountBO> deactivate(final String accountId) {
        return getById(accountId)
                .map(account -> account.withActive(false))
                .flatMap(this::update);
    }

    @Override
    public Optional<AccountBO> removeEmails(final String accountId, final List<String> emails) {
        final AccountBO existing = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + accountId + " was found"));

        final List<AccountEmailBO> newEmails = existing.getEmails().stream()
                .map(email -> {
                    if (emails.contains(email.getEmail())) {
                        return email.withActive(false);
                    }

                    return email;
                })
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
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

        final List<String> newEmails = emails.stream()
                .map(AccountEmailBO::getEmail)
                .collect(Collectors.toList());

        final Stream<AccountEmailBO> nonUpdatedEmails = existing.getEmails().stream()
                .filter(old -> !newEmails.contains(old.getEmail()));

        final Stream<AccountEmailBO> emailsWithActive = emails.stream()
                .map(email -> email.withActive(true));

        final List<AccountEmailBO> combinedList = Stream.concat(emailsWithActive, nonUpdatedEmails)
                .collect(Collectors.toList());

        final AccountBO updated = AccountBO.builder().from(existing)
                .emails(combinedList)
                .build();

        return update(updated);
    }

    @Override
    public List<PermissionBO> getPermissions(final String accountId) {
        final AccountBO account = getById(accountId)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

        return permissionsAggregator.aggregate(account.getRoles(), account.getPermissions());
    }

    @Override
    public AccountBO grantPermissions(final String accountId, final List<PermissionBO> permissions) {
        final List<PermissionBO> verifiedPermissions = permissionsService.validate(permissions);

        if (verifiedPermissions.size() != permissions.size()) {
            final List<PermissionBO> difference = permissions.stream()
                    .filter(permission -> !verifiedPermissions.contains(permission))
                    .collect(Collectors.toList());

            throw new ServiceException(ErrorCode.PERMISSION_DOES_NOT_EXIST, "The following permissions are not valid" + difference);
        }

        final AccountBO account = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

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
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

        final List<PermissionBO> filteredPermissions = account.getPermissions().stream()
                .filter(permission -> !permissions.contains(permission))
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(filteredPermissions);

        accountsRepository.update(serviceMapper.toDO(updated));

        return updated;
    }

    @Override
    public AccountBO grantRoles(final String accountId, final List<String> roles) {
        final List<String> verifiedRoles = rolesService.verifyRoles(roles);

        if (verifiedRoles.size() != roles.size()) {
            final List<String> difference = roles.stream()
                    .filter(role -> !verifiedRoles.contains(role))
                    .collect(Collectors.toList());

            throw new ServiceException(ErrorCode.ROLE_DOES_NOT_EXIST, "The following roles are not valid " + difference);
        }

        final AccountBO account = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

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
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

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
        return accountsRepository.getByRole(accountConfig.getAuthguardAdminRole())
                .join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }
}
