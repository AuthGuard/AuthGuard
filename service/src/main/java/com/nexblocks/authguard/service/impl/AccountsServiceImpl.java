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
import com.nexblocks.authguard.service.util.AccountPreProcessor;
import com.nexblocks.authguard.service.util.AccountUpdateMerger;
import com.nexblocks.authguard.service.util.CredentialsManager;
import com.nexblocks.authguard.service.util.ValueComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccountsServiceImpl implements AccountsService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountsServiceImpl.class);

    private static final String ACCOUNTS_CHANNEL = "accounts";
    private static final String VERIFICATION_CHANNEL = "verification";

    private final AccountsRepository accountsRepository;
    private final PermissionsService permissionsService;
    private final RolesService rolesService;
    private final CredentialsManager credentialsManager;
    private final IdempotencyService idempotencyService;
    private final AccountConfig accountConfig;
    private final ServiceMapper serviceMapper;
    private final MessageBus messageBus;
    private final PersistenceService<AccountBO, AccountDO, AccountsRepository> persistenceService;

    @Inject
    public AccountsServiceImpl(final AccountsRepository accountsRepository,
                               final PermissionsService permissionsService,
                               final RolesService rolesService,
                               final CredentialsManager credentialsManager,
                               final IdempotencyService idempotencyService,
                               final ServiceMapper serviceMapper,
                               final MessageBus messageBus,
                               final @Named("accounts") ConfigContext accountConfigContext) {
        this.accountsRepository = accountsRepository;
        this.permissionsService = permissionsService;
        this.rolesService = rolesService;
        this.credentialsManager = credentialsManager;
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
        final AccountBO withHashedPasswords = credentialsManager.verifyAndHashPlainPassword(account);
        final AccountBO preProcessed = AccountPreProcessor.preProcess(withHashedPasswords, accountConfig);

        verifyRolesOrFail(preProcessed.getRoles(), preProcessed.getDomain());

        final AccountBO created = persistenceService.create(preProcessed);

        if (accountConfig.verifyEmail()) {
            final List<AccountEmailBO> toVerify = new ArrayList<>(2);

            if (preProcessed.getEmail() != null) {
                toVerify.add(preProcessed.getEmail());
            }

            if (preProcessed.getBackupEmail() != null) {
                toVerify.add(preProcessed.getBackupEmail());
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

        return credentialsManager.removeSensitiveInformation(created);
    }

    @Override
    public Optional<AccountBO> getById(final long accountId) {
        return persistenceService.getById(accountId)
                .map(credentialsManager::removeSensitiveInformation);
    }

    @Override
    public Optional<AccountBO> getByIdUnsafe(final long id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<AccountBO> getByExternalId(final String externalId) {
        return accountsRepository.getByExternalId(externalId)
                .join()
                .map(serviceMapper::toBO)
                .map(credentialsManager::removeSensitiveInformation);
    }

    @Override
    public Optional<AccountBO> getByEmail(final String email, final String domain) {
        return accountsRepository.getByEmail(email, domain)
                .join()
                .map(serviceMapper::toBO)
                .map(credentialsManager::removeSensitiveInformation);
    }

    @Override
    public Optional<AccountBO> getByIdentifier(final String identifier, final String domain) {
        return accountsRepository.findByIdentifier(identifier, domain)
                .join()
                .map(serviceMapper::toBO)
                .map(credentialsManager::removeSensitiveInformation);
    }

    @Override
    public Optional<AccountBO> getByIdentifierUnsafe(final String identifier, final String domain) {
        return accountsRepository.findByIdentifier(identifier, domain)
                .join()
                .map(serviceMapper::toBO);
    }

    @Override
    public Optional<AccountBO> update(final AccountBO account) {
        LOG.info("Account update request. accountId={}, domain={}", account.getId(), account.getDomain());

        return persistenceService.update(account);
    }

    @Override
    public Optional<AccountBO> delete(final long accountId) {
        LOG.info("Account delete request. accountId={}", accountId);

        return persistenceService.delete(accountId);
    }

    @Override
    public Optional<AccountBO> activate(final long accountId) {
        return getByIdUnsafe(accountId)
                .flatMap(account -> {
                    LOG.info("Activate account request. accountId={}, domain={}", account.getId(), account.getDomain());
                    final AccountBO activated = account.withActive(true);
                    final Optional<AccountBO> persisted = this.update(activated);

                    if (persisted.isPresent()) {
                        LOG.info("Account activated. accountId={}, domain={}", account.getId(), account.getDomain());
                    } else {
                        LOG.info("Failed to activate account. accountId={}, domain={}", account.getId(), account.getDomain());
                    }

                    return persisted;
                });
    }

    @Override
    public Optional<AccountBO> deactivate(final long accountId) {
        return getByIdUnsafe(accountId)
                .flatMap(account -> {
                    LOG.info("Deactivate account request. accountId={}, domain={}", account.getId(), account.getDomain());
                    final AccountBO deactivated = account.withActive(false);
                    final Optional<AccountBO> persisted = this.update(deactivated);

                    if (persisted.isPresent()) {
                        LOG.info("Account deactivated. accountId={}, domain={}", account.getId(), account.getDomain());
                    } else {
                        LOG.info("Failed to deactivate account. accountId={}, domain={}", account.getId(), account.getDomain());
                    }

                    return persisted;
                });
    }

    @Override
    public Optional<AccountBO> patch(final long accountId, final AccountBO account) {
        final AccountBO existing = getByIdUnsafe(accountId)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID "
                        + accountId + " was found"));

        AccountBO merged = AccountUpdateMerger.merge(existing, account);

        final boolean emailUpdated = !ValueComparator.emailsEqual(existing.getEmail(), merged.getEmail());
        final boolean backupEmailUpdated = !ValueComparator.emailsEqual(existing.getBackupEmail(), merged.getBackupEmail());
        final boolean phoneNumberUpdated = !ValueComparator.phoneNumbersEqual(existing.getPhoneNumber(), merged.getPhoneNumber());

        LOG.info("Account patch request. accountId={}, domain={}, emailUpdate={}, backupEmailUpdate={}, phoneNumberUpdate={}",
                existing.getId(), existing.getDomain(), emailUpdated, backupEmailUpdated, phoneNumberUpdated);

        if (emailUpdated) {
            final String oldEmail = Optional.ofNullable(existing.getEmail())
                    .map(AccountEmailBO::getEmail)
                    .orElse(null);

            merged = credentialsManager.addOrReplaceIdentifier(
                    merged,
                    oldEmail,
                    merged.getEmail().getEmail(),
                    UserIdentifier.Type.EMAIL);
        }

        if (phoneNumberUpdated) {
            final String oldPhoneNumber = Optional.ofNullable(existing.getPhoneNumber())
                    .map(PhoneNumberBO::getNumber)
                    .orElse(null);

            merged = credentialsManager.addOrReplaceIdentifier(
                    merged,
                    oldPhoneNumber,
                    merged.getPhoneNumber().getNumber(),
                    UserIdentifier.Type.PHONE_NUMBER);
        }

        final AccountBO accountUpdate = merged;
        final Optional<AccountBO> updated = update(accountUpdate);

        updated.ifPresent(updatedAccount -> {
            // we could merge both email and backup email messages, but we kept them separate for now
            if (emailUpdated) {
                messageBus.publish(VERIFICATION_CHANNEL, Messages.emailVerification(
                        VerificationRequestBO.builder()
                                .account(updatedAccount)
                                .emails(Collections.singletonList(accountUpdate.getEmail()))
                                .build()));
            }

            if (backupEmailUpdated) {
                messageBus.publish(VERIFICATION_CHANNEL, Messages.emailVerification(
                        VerificationRequestBO.builder()
                                .account(updatedAccount)
                                .emails(Collections.singletonList(accountUpdate.getBackupEmail()))
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
    public Optional<AccountBO> grantPermissions(final long accountId, final List<PermissionBO> permissions) {
        final AccountBO account = getByIdUnsafe(accountId)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

        final List<PermissionBO> verifiedPermissions = permissionsService.validate(permissions, account.getDomain());

        LOG.info("Grant account permissions request. accountId={}, domain={}, permissions={}",
                account.getId(), account.getDomain(), verifiedPermissions);

        if (verifiedPermissions.size() != permissions.size()) {
            final List<PermissionBO> difference = permissions.stream()
                    .filter(permission -> !verifiedPermissions.contains(permission))
                    .collect(Collectors.toList());

            throw new ServiceException(ErrorCode.PERMISSION_DOES_NOT_EXIST, "The following permissions are not valid" + difference);
        }

        final List<PermissionBO> combinedPermissions = Stream.concat(account.getPermissions().stream(), verifiedPermissions.stream())
                .distinct()
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(combinedPermissions);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .join()
                .map(accountDO -> {
                    LOG.info("Granted account permissions. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), verifiedPermissions);

                    return serviceMapper.toBO(accountDO);
                });
    }

    @Override
    public Optional<AccountBO> revokePermissions(final long accountId, final List<PermissionBO> permissions) {
        final Set<String> permissionsFullNames = permissions.stream()
                .map(Permission::getFullName)
                .collect(Collectors.toSet());

        final AccountBO account = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

        LOG.info("Revoke account permissions request. accountId={}, domain={}, permissions={}",
                account.getId(), account.getDomain(), permissionsFullNames);

        final List<PermissionBO> filteredPermissions = account.getPermissions().stream()
                .filter(permission -> !permissionsFullNames.contains(permission.getFullName()))
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(filteredPermissions);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .join()
                .map(accountDO -> {
                    LOG.info("Revoked account permissions. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), permissionsFullNames);

                    return serviceMapper.toBO(accountDO);
                });
    }

    @Override
    public Optional<AccountBO> grantRoles(final long accountId, final List<String> roles) {
        final AccountBO account = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

        verifyRolesOrFail(roles, account.getDomain());

        LOG.info("Grant account roles request. accountId={}, domain={}, permissions={}",
                account.getId(), account.getDomain(), roles);

        final List<String> combinedRoles = Stream.concat(account.getRoles().stream(), roles.stream())
                .distinct()
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(combinedRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .join()
                .map(accountDO -> {
                    LOG.info("Granted account roles request. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), roles);

                    return serviceMapper.toBO(accountDO);
                });
    }

    @Override
    public Optional<AccountBO> revokeRoles(final long accountId, final List<String> roles) {
        final AccountBO account = accountsRepository.getById(accountId)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " 
                        + accountId + " was found"));

        LOG.info("Revoke account roles request. accountId={}, domain={}, permissions={}",
                account.getId(), account.getDomain(), roles);

        final List<String> filteredRoles = account.getRoles().stream()
                .filter(role -> !roles.contains(role))
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(filteredRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .join()
                .map(accountDO -> {
                    LOG.info("Revoked account roles. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), roles);

                    return serviceMapper.toBO(accountDO);
                });
    }

    @Override
    public List<AccountBO> getAdmins() {
        return getByRole(accountConfig.getAuthguardAdminRole(), "global");
    }

    @Override
    public List<AccountBO> getByRole(final String role, final String domain) {
        return accountsRepository.getByRole(role, domain)
                .join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    private void verifyRolesOrFail(final Collection<String> roles, final String domain) {
        final List<String> verifiedRoles = rolesService.verifyRoles(roles, domain);

        if (verifiedRoles.size() != roles.size()) {
            final List<String> difference = roles.stream()
                    .filter(role -> !verifiedRoles.contains(role))
                    .collect(Collectors.toList());

            throw new ServiceException(ErrorCode.ROLE_DOES_NOT_EXIST, "The following roles are not valid " + difference);
        }
    }
}
