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
import java.util.concurrent.CompletableFuture;
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
    public CompletableFuture<AccountBO> create(final AccountBO account, final RequestContextBO requestContext) {
        return idempotencyService
                .performOperationAsync(() -> doCreate(account), requestContext.getIdempotentKey(), account.getEntityType());
    }

    private CompletableFuture<AccountBO> doCreate(final AccountBO account) {
        final AccountBO withHashedPasswords = credentialsManager.verifyAndHashPlainPassword(account);
        final AccountBO preProcessed = AccountPreProcessor.preProcess(withHashedPasswords, accountConfig);

        verifyRolesOrFail(preProcessed.getRoles(), preProcessed.getDomain());

        return persistenceService.create(preProcessed)
                .thenApply(created -> {
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
                });
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> getById(final long accountId) {
        return persistenceService.getById(accountId)
                .thenApply(opt -> opt.map(credentialsManager::removeSensitiveInformation));
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> getByIdUnsafe(final long id) {
        return persistenceService.getById(id);
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> getByExternalId(final String externalId) {
        return accountsRepository.getByExternalId(externalId)
                .thenApply(opt -> opt.map(serviceMapper::toBO)
                        .map(credentialsManager::removeSensitiveInformation));
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> getByEmail(final String email, final String domain) {
        return accountsRepository.getByEmail(email, domain)
                .thenApply(opt -> opt
                        .map(serviceMapper::toBO)
                        .map(credentialsManager::removeSensitiveInformation));
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> getByIdentifier(final String identifier, final String domain) {
        return accountsRepository.findByIdentifier(identifier, domain)
                .thenApply(opt -> opt
                        .map(serviceMapper::toBO)
                        .map(credentialsManager::removeSensitiveInformation));
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> getByIdentifierUnsafe(final String identifier, final String domain) {
        return accountsRepository.findByIdentifier(identifier, domain)
                .thenApply(opt -> opt.map(serviceMapper::toBO));
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> update(final AccountBO account) {
        LOG.info("Account update request. accountId={}, domain={}", account.getId(), account.getDomain());

        return persistenceService.update(account);
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> delete(final long accountId) {
        LOG.info("Account delete request. accountId={}", accountId);

        return persistenceService.delete(accountId);
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> activate(final long accountId) {
        return getByIdUnsafe(accountId)
                .thenCompose(opt -> {
                    if (opt.isPresent()) {
                        AccountBO activated = opt.get().withActive(true);
                        LOG.info("Activate account request. accountId={}, domain={}", activated.getId(), activated.getDomain());

                        return this.update(activated);
                    }

                    return CompletableFuture.completedFuture(opt);
                }).thenApply(persisted -> {
                    if (persisted.isPresent()) {
                        LOG.info("Account activated. accountId={}, domain={}", accountId, persisted.get().getDomain());
                    } else {
                        LOG.info("Failed to activate account. accountId={}", accountId);
                    }

                    return persisted;
                });
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> deactivate(final long accountId) {
        return getByIdUnsafe(accountId)
                .thenCompose(opt -> {
                    if (opt.isPresent()) {
                        AccountBO deactivated = opt.get().withActive(false);
                        LOG.info("Deactivate account request. accountId={}, domain={}", deactivated.getId(), deactivated.getDomain());

                        return this.update(deactivated);
                    }

                    return CompletableFuture.completedFuture(opt);
                }).thenApply(persisted -> {
                    if (persisted.isPresent()) {
                        LOG.info("Account deactivated. accountId={}, domain={}", persisted.get().getId(), persisted.get().getDomain());
                    } else {
                        LOG.info("Failed to deactivate account. accountId={}", accountId);
                    }

                    return persisted;
                });
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> patch(final long accountId, final AccountBO account) {
        return getByIdUnsafe(accountId)
                .thenCompose(opt -> {
                    AccountBO existing = opt
                            .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID "
                                    + accountId + " was found"));

                    AccountBO merged = AccountUpdateMerger.merge(existing, account);

                    boolean emailUpdated = !ValueComparator.emailsEqual(existing.getEmail(), merged.getEmail());
                    boolean backupEmailUpdated = !ValueComparator.emailsEqual(existing.getBackupEmail(), merged.getBackupEmail());
                    boolean phoneNumberUpdated = !ValueComparator.phoneNumbersEqual(existing.getPhoneNumber(), merged.getPhoneNumber());

                    LOG.info("Account patch request. accountId={}, domain={}, emailUpdate={}, backupEmailUpdate={}, phoneNumberUpdate={}",
                            existing.getId(), existing.getDomain(), emailUpdated, backupEmailUpdated, phoneNumberUpdated);

                    if (emailUpdated) {
                        String oldEmail = Optional.ofNullable(existing.getEmail())
                                .map(AccountEmailBO::getEmail)
                                .orElse(null);

                        merged = credentialsManager.addOrReplaceIdentifier(
                                merged,
                                oldEmail,
                                merged.getEmail().getEmail(),
                                UserIdentifier.Type.EMAIL);
                    }

                    if (phoneNumberUpdated) {
                        String oldPhoneNumber = Optional.ofNullable(existing.getPhoneNumber())
                                .map(PhoneNumberBO::getNumber)
                                .orElse(null);

                        merged = credentialsManager.addOrReplaceIdentifier(
                                merged,
                                oldPhoneNumber,
                                merged.getPhoneNumber().getNumber(),
                                UserIdentifier.Type.PHONE_NUMBER);
                    }

                    AccountBO accountUpdate = merged;
                    return update(accountUpdate)
                            .thenApply(updated -> {
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
                            });
                });
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> grantPermissions(final long accountId, final List<PermissionBO> permissions) {
        return getByIdUnsafe(accountId)
                .thenCompose(opt -> {
                    AccountBO account = opt.orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                            "No account with ID " + accountId + " was found"));

                    List<PermissionBO> verifiedPermissions = permissionsService.validate(permissions, account.getDomain());

                    LOG.info("Grant account permissions request. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), verifiedPermissions);

                    if (verifiedPermissions.size() != permissions.size()) {
                        List<PermissionBO> difference = permissions.stream()
                                .filter(permission -> !verifiedPermissions.contains(permission))
                                .collect(Collectors.toList());

                        throw new ServiceException(ErrorCode.PERMISSION_DOES_NOT_EXIST, "The following permissions are not valid" + difference);
                    }

                    List<PermissionBO> combinedPermissions = Stream.concat(account.getPermissions().stream(), verifiedPermissions.stream())
                            .distinct()
                            .collect(Collectors.toList());

                    AccountBO withNewPermissions = account.withPermissions(combinedPermissions);

                    return accountsRepository.update(serviceMapper.toDO(withNewPermissions))
                            .thenApply(updated -> updated.map(accountDO -> {
                                LOG.info("Granted account permissions. accountId={}, domain={}, permissions={}",
                                        account.getId(), account.getDomain(), verifiedPermissions);

                                return serviceMapper.toBO(accountDO);
                            }));
                });
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> revokePermissions(final long accountId, final List<PermissionBO> permissions) {
        return getByIdUnsafe(accountId)
                .thenCompose(opt -> {
                    AccountBO account = opt.orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                            "No account with ID " + accountId + " was found"));

                    Set<String> permissionsFullNames = permissions.stream()
                            .map(Permission::getFullName)
                            .collect(Collectors.toSet());

                    LOG.info("Revoke account permissions request. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), permissionsFullNames);

                    List<PermissionBO> filteredPermissions = account.getPermissions().stream()
                            .filter(permission -> !permissionsFullNames.contains(permission.getFullName()))
                            .collect(Collectors.toList());

                    AccountBO withNewPermissions = account.withPermissions(filteredPermissions);

                    return accountsRepository.update(serviceMapper.toDO(withNewPermissions))
                            .thenApply(updated -> updated.map(accountDO -> {
                                        LOG.info("Revoked account permissions. accountId={}, domain={}, permissions={}",
                                                account.getId(), account.getDomain(), permissionsFullNames);

                                        return serviceMapper.toBO(accountDO);
                                    }));
                });
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> grantRoles(final long accountId, final List<String> roles) {
        return getByIdUnsafe(accountId)
                .thenCompose(opt -> {
                    AccountBO account = opt.orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                            "No account with ID " + accountId + " was found"));

                    verifyRolesOrFail(roles, account.getDomain());

                    LOG.info("Grant account roles request. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), roles);

                    List<String> combinedRoles = Stream.concat(account.getRoles().stream(), roles.stream())
                            .distinct()
                            .collect(Collectors.toList());

                    AccountBO withNewRoles = account.withRoles(combinedRoles);

                    return accountsRepository.update(serviceMapper.toDO(withNewRoles))
                            .thenApply(updated -> updated.map(accountDO -> {
                                        LOG.info("Granted account roles request. accountId={}, domain={}, permissions={}",
                                                account.getId(), account.getDomain(), roles);

                                        return serviceMapper.toBO(accountDO);
                                    }));
                });
    }

    @Override
    public CompletableFuture<Optional<AccountBO>> revokeRoles(final long accountId, final List<String> roles) {
        return getByIdUnsafe(accountId)
                .thenCompose(opt -> {
                    AccountBO account = opt.orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                            "No account with ID " + accountId + " was found"));

                    LOG.info("Revoke account roles request. accountId={}, domain={}, permissions={}",
                            account.getId(), account.getDomain(), roles);

                    List<String> filteredRoles = account.getRoles().stream()
                            .filter(role -> !roles.contains(role))
                            .collect(Collectors.toList());

                    AccountBO withNewRoles = account.withRoles(filteredRoles);

                    return accountsRepository.update(serviceMapper.toDO(withNewRoles))
                            .thenApply(updated -> updated.map(accountDO -> {
                                LOG.info("Revoked account roles. accountId={}, domain={}, permissions={}",
                                        account.getId(), account.getDomain(), roles);

                                return serviceMapper.toBO(accountDO);
                            }));
                });
    }

    @Override
    public CompletableFuture<List<AccountBO>> getAdmins() {
        return getByRole(accountConfig.getAuthguardAdminRole(), "global");
    }

    @Override
    public CompletableFuture<List<AccountBO>> getByRole(final String role, final String domain) {
        return accountsRepository.getByRole(role, domain)
                .thenApply(accounts -> accounts.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
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
