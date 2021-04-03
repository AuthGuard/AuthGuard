package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.model.AccountDO;
import com.nexblocks.authguard.dal.persistence.AccountsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.AccountUpdateMerger;
import com.nexblocks.authguard.service.util.ValueComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccountsServiceImpl implements AccountsService {
    private static final String ACCOUNTS_CHANNEL = "accounts";
    private static final String VERIFICATION_CHANNEL = "verification";

    private final AccountsRepository accountsRepository;
    private final PermissionsService permissionsService;
    private final RolesService rolesService;
    private final IdempotencyService idempotencyService;
    private final AccountConfig accountConfig;
    private final ServiceMapper serviceMapper;
    private final MessageBus messageBus;
    private final PersistenceService<AccountBO, AccountDO, AccountsRepository> persistenceService;

    @Inject
    public AccountsServiceImpl(final AccountsRepository accountsRepository,
                               final PermissionsService permissionsService,
                               final RolesService rolesService,
                               final IdempotencyService idempotencyService,
                               final ServiceMapper serviceMapper,
                               final MessageBus messageBus,
                               final @Named("accounts") ConfigContext accountConfigContext) {
        this.accountsRepository = accountsRepository;
        this.permissionsService = permissionsService;
        this.rolesService = rolesService;
        this.idempotencyService = idempotencyService;
        this.serviceMapper = serviceMapper;
        this.messageBus = messageBus;
        this.accountConfig = accountConfigContext.asConfigBean(AccountConfig.class);

        this.persistenceService = new PersistenceService<>(accountsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, ACCOUNTS_CHANNEL);
    }

    @Override
    public AccountBO create(final AccountBO account, final RequestContextBO requestContext) {
        return idempotencyService
                .performOperation(() -> doCreate(account), requestContext.getIdempotentKey(), account.getEntityType())
                .join();
    }

    private AccountBO doCreate(final AccountBO account) {
        final AccountBO created = persistenceService.create(account);

        if (accountConfig.verifyEmail()) {
            final List<AccountEmailBO> toVerify = new ArrayList<>(2);

            if (account.getEmail() != null) {
                toVerify.add(account.getEmail());
            }

            if (account.getBackupEmail() != null) {
                toVerify.add(account.getBackupEmail());
            }

            messageBus.publish(VERIFICATION_CHANNEL, Messages.emailVerification(VerificationRequestBO.builder()
                    .account(created)
                    .emails(toVerify)
                    .build()));
        }

        if (accountConfig.verifyPhoneNumber()) {
            /*
             * Unlike emails, we only have a single phone number. Therefore, we don't
             * need to specify which ones to verify.
             */
            messageBus.publish(VERIFICATION_CHANNEL, Messages.phoneNumberVerification(VerificationRequestBO.builder()
                    .account(created)
                    .build()));
        }

        return created;
    }

    @Override
    public Optional<AccountBO> getById(final String accountId) {
        return persistenceService.getById(accountId);
    }

    @Override
    public Optional<AccountBO> getByExternalId(final String externalId) {
        return accountsRepository.getByExternalId(externalId)
                .join()
                .map(serviceMapper::toBO);
    }

    @Override
    public Optional<AccountBO> getByEmail(final String email) {
        return accountsRepository.getByEmail(email)
                .join()
                .map(serviceMapper::toBO);
    }

    @Override
    public Optional<AccountBO> update(final AccountBO account) {
        return persistenceService.update(account);
    }

    @Override
    public Optional<AccountBO> delete(final String accountId) {
        return persistenceService.delete(accountId);
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
    public Optional<AccountBO> patch(final String accountId, final AccountBO account) {
        final AccountBO existing = getById(accountId)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID "
                        + accountId + " was found"));

        final AccountBO merged = AccountUpdateMerger.merge(existing, account);

        final boolean emailUpdated = !ValueComparator.emailsEqual(existing.getEmail(), merged.getEmail());
        final boolean backupEmailUpdated = !ValueComparator.emailsEqual(existing.getBackupEmail(), merged.getBackupEmail());
        final boolean phoneNumberUpdated = !ValueComparator.phoneNumbersEqual(existing.getPhoneNumber(), merged.getPhoneNumber());

        final Optional<AccountBO> updated = update(merged);

        updated.ifPresent(updatedAccount -> {
            // we could merge both email and backup email messages, but we kept them separate for now
            if (emailUpdated) {
                messageBus.publish(VERIFICATION_CHANNEL, Messages.emailVerification(
                        VerificationRequestBO.builder()
                                .account(updatedAccount)
                                .emails(Collections.singletonList(merged.getEmail()))
                                .build()));
            }

            if (backupEmailUpdated) {
                messageBus.publish(VERIFICATION_CHANNEL, Messages.emailVerification(
                        VerificationRequestBO.builder()
                                .account(updatedAccount)
                                .emails(Collections.singletonList(merged.getBackupEmail()))
                                .build()));
            }

            if (phoneNumberUpdated) {
                messageBus.publish(VERIFICATION_CHANNEL, Messages.phoneNumberVerification(
                        VerificationRequestBO.builder()
                                .account(updatedAccount)
                                .build()));
            }
        });

        return updated;
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

        final AccountBO account = getById(accountId)
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
